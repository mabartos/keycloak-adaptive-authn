package io.github.mabartos.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    protected abstract Risk evaluateRiskContinuous(@Nonnull RealmModel realm, @Nonnull UserModel knownUser);

    protected abstract Risk evaluateRiskBeforeAuthn(@Nonnull RealmModel realm);

    protected abstract Risk evaluateRiskUserKnown(@Nonnull RealmModel realm, @Nonnull UserModel knownUser);

    @Override
    public Risk evaluateRisk(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (!isRiskBasedAuthnEnabled()) {
            return Risk.invalid("Risk-based authentication is disabled. Skipping risk evaluation.");
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
            case CONTINUOUS -> evaluateRiskContinuous(realm, Objects.requireNonNull(knownUser));
            case BEFORE_AUTHN -> evaluateRiskBeforeAuthn(realm);
            case USER_KNOWN -> evaluateRiskUserKnown(realm, Objects.requireNonNull(knownUser));
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
    public Set<RiskEvaluator> getRiskEvaluators(@Nonnull RiskEvaluator.EvaluationPhase evaluationPhase, @Nonnull RealmModel realm) {
        return riskEvaluators.get(evaluationPhase)
                .stream()
                .filter(f -> f.isEnabled(realm))
                .collect(Collectors.toSet());
    }

    protected static Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> initializeRiskEvaluators(KeycloakSession session) {
        Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators = new EnumMap<>(RiskEvaluator.EvaluationPhase.class);
        for (RiskEvaluator.EvaluationPhase phase : RiskEvaluator.EvaluationPhase.values()) {
            riskEvaluators.put(phase, new LinkedHashSet<>());
        }

        session.getAllProviders(RiskEvaluator.class)
                .forEach(f -> f.evaluationPhases().forEach(phase -> riskEvaluators.get(phase).add(f)));

        return riskEvaluators;
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(RealmModel realm, String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Helper class to track individual evaluator execution results
     */
    protected static class EvaluatorResult {
        private final String evaluatorName;
        private final String score;
        private final String weight;
        private final long durationMs;

        public EvaluatorResult(String evaluatorName, String score, String weight, long durationMs) {
            this.evaluatorName = evaluatorName;
            this.score = score;
            this.weight = weight;
            this.durationMs = durationMs;
        }

        public String format() {
            return String.format("Evaluator: %s - Risk score: '%s' (weight '%s') - %d ms",
                    evaluatorName, score, weight, durationMs);
        }
    }

    /**
     * Helper class to collect evaluator results in order
     */
    protected static class EvaluatorResults {
        private final java.util.List<EvaluatorResult> results = new java.util.concurrent.CopyOnWriteArrayList<>();

        public void add(EvaluatorResult result) {
            results.add(result);
        }

        public void logAll() {
            results.forEach(r -> logger.debug(r.format()));
        }
    }

    /**
     * Executes an evaluator with timing and collects the result
     *
     * @param evaluator the evaluator to execute
     * @param realm the realm
     * @param knownUser the user (can be null)
     * @param retries number of retries allowed
     * @param results optional results collector
     */
    protected void executeEvaluator(@Nonnull RiskEvaluator evaluator, @Nonnull RealmModel realm, @Nullable UserModel knownUser, int retries, @Nullable EvaluatorResults results) {
        var startTime = org.keycloak.common.util.Time.currentTimeMillis();
        try {
            var retriesCount = evaluator.allowRetries() ? retries : 1;
            for (int i = 0; i < retriesCount; i++) {
                try {
                    evaluator.evaluateRisk(realm, knownUser);
                    if (evaluator.getRisk().isValid()) {
                        break;
                    }
                } catch (Exception e) {
                    logger.warnf("Evaluator %s failed on attempt %d: %s", evaluator.getClass().getSimpleName(), i + 1, e.getMessage());
                    if (i == retriesCount - 1) {
                        logger.errorf("Evaluator %s failed after %d retries", evaluator.getClass().getSimpleName(), retriesCount);
                    }
                }
            }
        } finally {
            var duration = org.keycloak.common.util.Time.currentTimeMillis() - startTime;
            if (results != null) {
                var score = evaluator.getRisk().getScore().map(s -> String.format("%.2f", s)).orElse("N/A");
                var weight = String.format("%.6f", evaluator.getWeight(realm));
                results.add(new EvaluatorResult(evaluator.getClass().getSimpleName(), score, weight, duration));
            }
        }
    }
}
