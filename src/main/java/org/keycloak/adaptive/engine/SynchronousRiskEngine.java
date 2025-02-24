package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SynchronousRiskEngine implements RiskEngine {

    private static final Logger logger = Logger.getLogger(SynchronousRiskEngine.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final TracingProvider tracingProvider;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private final StoredRiskProvider storedRiskProvider;

    private boolean requiresUser;
    private StoredRiskProvider.RiskPhase riskPhase;
    private Double risk;

    public SynchronousRiskEngine(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.riskFactorEvaluators = session.getAllProviders(RiskEvaluator.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    public Double getRisk() {
        return risk;
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators.stream().filter(f -> f.requiresUser() == requiresUser).filter(RiskEvaluator::isEnabled).collect(Collectors.toSet());
    }

    @Override
    public void evaluateRisk() {
        logger.debugf("Risk Engine - EVALUATING");

        // It is not necessary to evaluate the risk multiple times for 'NO_USER' phase
        if (!requiresUser) {
            final var storedRisk = storedRiskProvider.getStoredRisk(StoredRiskProvider.RiskPhase.NO_USER);
            if (storedRisk.isPresent()) {
                logger.debugf("Risk for the phase 'NO_USER' was already evaluated (score: %f). Skipping the evaluation", storedRisk.get());
                this.risk = storedRisk.get();
                return;
            }
        }

        tracingProvider.trace(SynchronousRiskEngine.class, "evaluateAll", span -> {
            getRiskEvaluators().forEach(RiskEvaluator::evaluateRisk);

            var weightedRisk = getRiskEvaluators().stream()
                    .filter(eval -> eval.getRiskScore().isPresent())
                    .peek(eval -> logger.debugf("Evaluator: %s", eval.getClass().getSimpleName()))
                    .peek(eval -> logger.debugf("Risk evaluated: %f (weight %f)", eval.getRiskScore().get(), eval.getWeight()))
                    .mapToDouble(eval -> eval.getRiskScore().get() * eval.getWeight())
                    .sum();

            var weights = getRiskEvaluators().stream()
                    .mapToDouble(RiskEvaluator::getWeight)
                    .sum();

            this.risk = weightedRisk / weights;
            logger.debugf("SYNC - the overall risk score is %f - (requires user: %s)", risk, requiresUser);

            storedRiskProvider.storeRisk(risk, riskPhase);
        });
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        this.requiresUser = requiresUser(context.getAuthenticatorConfig());
        this.riskPhase = requiresUser ? StoredRiskProvider.RiskPhase.REQUIRES_USER : StoredRiskProvider.RiskPhase.NO_USER;

        final var storedRisk = storedRiskProvider.getStoredRisk(riskPhase);

        if (storedRisk.isPresent()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", riskPhase, storedRisk.get());
        } else {
            evaluateRisk();
        }

        context.success();
    }

    @Override
    public boolean requiresUser(AuthenticatorConfigModel configModel) {
        return Optional.ofNullable(configModel)
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(SynchronousRiskEngineFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }
}
