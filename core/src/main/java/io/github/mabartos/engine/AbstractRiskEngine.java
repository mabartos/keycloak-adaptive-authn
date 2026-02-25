package io.github.mabartos.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG;

public abstract class AbstractRiskEngine implements RiskEngine {
    protected static final Logger logger = Logger.getLogger(AbstractRiskEngine.class);
    // TODO have it configurable
    protected static final double RISK_THRESHOLD_LOG_OUT_USER = 0.8;

    protected final KeycloakSession session;
    protected final TracingProvider tracingProvider;
    protected final RiskScoreAlgorithm riskScoreAlgorithm;
    protected final Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators;
    protected final Set<UserContext> userContexts;
    protected final StoredRiskProvider storedRiskProvider;

    protected Risk risk = Risk.invalid();

    public AbstractRiskEngine(KeycloakSession session) {
        this.session = session;
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.riskScoreAlgorithm = session.getProvider(RiskScoreAlgorithm.class);
        this.userContexts = session.getAllProviders(UserContext.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
        this.riskEvaluators = initializeRiskEvaluators(session);
    }

    protected abstract Risk evaluateRiskContinuous(RealmModel realm, UserModel knownUser);

    protected abstract Risk evaluateRiskBeforeAuthn(RealmModel realm);

    protected abstract Risk evaluateRiskUserKnown(RealmModel realm, UserModel knownUser);

    @Override
    public Risk evaluateRisk(RiskEvaluator.EvaluationPhase evaluationPhase) {
        return evaluateRisk(evaluationPhase, null, null);
    }

    @Override
    public Risk evaluateRisk(RiskEvaluator.EvaluationPhase phase, RealmModel realm, UserModel knownUser) {
        if (!isRiskBasedAuthnEnabled()) {
            return Risk.invalid("Risk-based authentication is disabled. Skipping risk evaluation.");
        }

        if (realm == null) {
            return Risk.invalid("Cannot execute risk score evaluation, because the realm is null");
        }

        if (phase.requiresKnownUser && knownUser == null) {
            return Risk.invalid("Cannot execute risk score evaluation, because the user needs to be known");
        }

        final var storedRisk = storedRiskProvider.getStoredRisk(phase);
        if (storedRisk.isValid()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", phase.name(), storedRisk.getScore());
        }

        logger.debug("--------------------------------------------------");
        logger.debugf("Risk Engine ('%s') - EVALUATING (username: '%s', phase: %s)", getClass().getSimpleName(), knownUser != null ? knownUser.getUsername() : "N/A", phase.name());
        var start = Time.currentTimeMillis();

        var risk = switch (phase) {
            case CONTINUOUS -> evaluateRiskContinuous(realm, knownUser);
            case BEFORE_AUTHN -> evaluateRiskBeforeAuthn(realm);
            case USER_KNOWN -> evaluateRiskUserKnown(realm, knownUser);
        };

        logger.debugf("Risk Engine (Virtual Threads) - STOPPED EVALUATING (phase: %s) - consumed time: '%d ms'", phase.name(), Time.currentTimeMillis() - start);
        logger.debug("--------------------------------------------------");
        return risk;
    }

    @Override
    public boolean isRiskBasedAuthnEnabled() {
        var realm = session.getContext().getRealm();
        return Optional.ofNullable(realm.getAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    @Override
    public Risk getOverallRisk() {
        return risk;
    }

    @Override
    public Risk getRisk(RiskEvaluator.EvaluationPhase phase) {
        if (phase == null) {
            return Risk.invalid("Invalid evaluation phase");
        }
        return storedRiskProvider.getStoredRisk(phase);
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators(RiskEvaluator.EvaluationPhase phase) {
        if (phase == null) {
            logger.debug("Invalid evaluation phase - no risk evaluators returned");
            return Collections.emptySet();
        }
        return riskEvaluators.get(phase);
    }

    protected static Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> initializeRiskEvaluators(KeycloakSession session) {
        Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators = new EnumMap<>(RiskEvaluator.EvaluationPhase.class);
        for (RiskEvaluator.EvaluationPhase phase : RiskEvaluator.EvaluationPhase.values()) {
            riskEvaluators.put(phase, new LinkedHashSet<>());
        }

        session.getAllProviders(RiskEvaluator.class).stream()
                .filter(RiskEvaluator::isEnabled)
                .forEach(f -> f.evaluationPhases()
                        .forEach(phase -> riskEvaluators.get(phase).add(f)));

        return riskEvaluators;
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(RealmModel realm, String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
