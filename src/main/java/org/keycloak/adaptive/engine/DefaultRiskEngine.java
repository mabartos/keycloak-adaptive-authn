package org.keycloak.adaptive.engine;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;

public class DefaultRiskEngine implements RiskEngine {
    private static final Logger logger = Logger.getLogger(DefaultRiskEngine.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private final ExecutorsProvider executorsProvider;
    private final StoredRiskProvider storedRiskProvider;

    private boolean requiresUser;
    private StoredRiskProvider.RiskPhase riskPhase;
    private Double risk;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.riskFactorEvaluators = session.getAllProviders(RiskEvaluator.class);
        this.executorsProvider = session.getProvider(ExecutorsProvider.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    public void evaluateRisk() {
        logger.debugf("Risk Engine - EVALUATING");

        var start = Time.currentTimeMillis();
        var exec = executorsProvider.getExecutor("risk-engine");

        var timeout = getNumberRealmAttribute(EVALUATOR_TIMEOUT_CONFIG, Long::parseLong).orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        final var evaluators = Multi.createFrom()
                .items(getRiskEvaluators())
                .onItem()
                .transformToIterable(f -> f)
                .filter(RiskEvaluator::isEnabled)
                .filter(f -> f.requiresUser() == this.requiresUser)
                .collect()
                .asSet()
                .runSubscriptionOn(exec);

        final Function<RiskEvaluator, Uni<RiskEvaluator>> processEvaluator = (re) -> Uni.createFrom()
                .item(re)
                .onItem()
                .invoke(RiskEvaluator::evaluate)
                .onFailure()
                .retry()
                .atMost(retries)
                .ifNoItem()
                .after(Duration.ofMillis(timeout))
                .fail()
                .onFailure(TimeoutException.class)
                .recoverWithItem(() -> re)
                .emitOn(exec);

        evaluators.subscribe().with(e -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            var evaluatedRisks = Multi.createFrom()
                    .items(e.stream())
                    .onItem()
                    .transformToUniAndConcatenate(processEvaluator::apply)
                    .filter(f -> RiskEngine.isValidValue(f.getWeight()) && RiskEngine.isValidValue(f.getRiskValue()))
                    .collect()
                    .asSet();

            evaluatedRisks.subscribe().with(risks -> {
                var weightedRisk = risks.stream()
                        .peek(g -> logger.debugf("Evaluator: %s", g.getClass().getSimpleName()))
                        .peek(g -> logger.debugf("Risk evaluated: %f (weight %f)", g.getRiskValue(), g.getWeight()))
                        .mapToDouble(g -> g.getRiskValue() * g.getWeight())
                        .sum();

                var weights = risks.stream()
                        .mapToDouble(RiskEvaluator::getWeight)
                        .sum();

                // Weighted mean
                this.risk = weightedRisk / weights;
                logger.debugf("The overall risk score is %f - (requires user: %s)", risk, requiresUser);

                storedRiskProvider.storeRisk(risk, riskPhase);
            });
        }), failure -> logger.error(failure.getCause()));
        logger.debugf("Consumed time: '%d ms'", Time.currentTimeMillis() - start);
    }

    @Override
    public Double getRisk() {
        return risk;
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var requiresUser = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(DefaultRiskEngineFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(null);

        if (requiresUser == null) {
            logger.warnf("Cannot find config '%s'", DefaultRiskEngineFactory.REQUIRES_USER_CONFIG);
            return;
        }

        this.requiresUser = requiresUser;
        this.riskPhase = requiresUser ? StoredRiskProvider.RiskPhase.REQUIRES_USER : StoredRiskProvider.RiskPhase.NO_USER;

        final var storedRisk = storedRiskProvider.getStoredRisk(riskPhase);

        if (storedRisk.isPresent()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", riskPhase, storedRisk.get());
        } else {
            evaluateRisk();
        }

        context.success();
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}