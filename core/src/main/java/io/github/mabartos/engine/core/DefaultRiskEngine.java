package io.github.mabartos.engine.core;

import io.github.mabartos.engine.FederatedStorageUserModelDelegate;
import io.opentelemetry.context.Context;
import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.engine.RemoteEvaluationContext;
import io.github.mabartos.spi.engine.RemoteEvaluatorExecutor;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.mabartos.spi.engine.RiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static io.github.mabartos.spi.engine.RiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static io.github.mabartos.spi.engine.RiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static io.github.mabartos.spi.engine.RiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_SCORE_ALGORITHM_CONFIG;

/**
 * Default risk engine that evaluates local evaluators sequentially and delegates
 * remote evaluators to the {@link RemoteEvaluatorExecutor} SPI.
 * <p>
 * If no remote executor is available, remote evaluators are skipped with a warning.
 */
public class DefaultRiskEngine implements RiskEngine {
    protected static final Logger logger = Logger.getLogger(DefaultRiskEngine.class);
    // TODO have it configurable
    protected static final double RISK_THRESHOLD_LOG_OUT_USER = 0.8;

    protected final KeycloakSession session;
    protected final TracingProvider tracingProvider;
    protected final RiskScoreAlgorithm defaultRiskScoreAlgorithm;
    protected final Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators;
    protected final Set<UserContext> userContexts;
    protected final StoredRiskProvider storedRiskProvider;
    protected final RiskEvaluationAuditPublisher auditPublisher;
    @Nullable
    protected final RemoteEvaluatorExecutor remoteExecutor;

    protected ResultRisk risk = ResultRisk.invalid();

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.defaultRiskScoreAlgorithm = session.getProvider(RiskScoreAlgorithm.class);
        this.userContexts = session.getAllProviders(UserContext.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
        this.auditPublisher = RiskEvaluationAuditPublisher.forSession(session);
        this.riskEvaluators = initializeRiskEvaluators(session);
        this.remoteExecutor = session.getProvider(RemoteEvaluatorExecutor.class);

        if (remoteExecutor != null) {
            logger.debugf("Remote evaluator executor: %s", remoteExecutor.getClass().getSimpleName());
        } else {
            logger.debug("No remote evaluator executor available — remote evaluators will be skipped");
        }
    }

    // --------------------------------------------------
    // RiskEngine interface
    // --------------------------------------------------

    @Override
    public ResultRisk evaluateRisk(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (!isRiskBasedAuthnEnabled(realm)) {
            return ResultRisk.invalid("Risk-based authentication is disabled. Skipping risk evaluation.");
        }

        if (phase.requiresKnownUser && knownUser == null) {
            return ResultRisk.invalid("Cannot execute risk score evaluation, because the user needs to be known");
        }

        knownUser = FederatedStorageUserModelDelegate.wrapIfNeeded(knownUser, session, realm);

        final var storedRisk = storedRiskProvider.getStoredRisk(phase);
        if (storedRisk.isValid()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", phase.name(), storedRisk.getScore());
        }

        logger.debug("--------------------------------------------------");
        logger.debugf("Risk Engine ('%s') - EVALUATING (username: '%s', phase: %s)", getClass().getSimpleName(), knownUser != null ? knownUser.getUsername() : "N/A", phase.name());
        var start = Time.currentTimeMillis();

        var risk = switch (phase) {
            case CONTINUOUS -> evaluateRiskContinuous(realm, Objects.requireNonNull(knownUser));
            case BEFORE_AUTHN -> evaluateRiskAuthentication(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, realm, null);
            case USER_KNOWN -> evaluateRiskAuthentication(RiskEvaluator.EvaluationPhase.USER_KNOWN, realm, Objects.requireNonNull(knownUser));
        };

        logger.debugf("Risk Engine ('%s') - STOPPED EVALUATING (phase: %s) - consumed time: '%d ms'", getClass().getSimpleName(), phase.name(), Time.currentTimeMillis() - start);
        logger.debug("--------------------------------------------------");
        return risk;
    }

    @Override
    public ResultRisk getOverallRisk() {
        return risk;
    }

    @Override
    public ResultRisk getRisk(RiskEvaluator.EvaluationPhase phase) {
        if (phase == null) {
            return ResultRisk.invalid("Invalid evaluation phase");
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

    @Override
    public RiskScoreAlgorithm getRiskScoreAlgorithm(@Nonnull RealmModel realm) {
        var realmAlgorithm = realm.getAttribute(RISK_SCORE_ALGORITHM_CONFIG);
        if (realmAlgorithm != null) {
            var provider = session.getProvider(RiskScoreAlgorithm.class, realmAlgorithm);
            if (provider != null) {
                return provider;
            }
        }
        return defaultRiskScoreAlgorithm;
    }

    @Override
    public boolean isRiskBasedAuthnEnabled(@Nonnull RealmModel realm) {
        return Optional.ofNullable(realm.getAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    // --------------------------------------------------
    // Evaluation orchestration
    // --------------------------------------------------

    protected ResultRisk evaluateRiskContinuous(@Nonnull RealmModel realm, @Nonnull UserModel knownUser) {
        var evaluators = getRiskEvaluators(RiskEvaluator.EvaluationPhase.CONTINUOUS, realm);

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s ->
                tracingProvider.trace(DefaultRiskEngine.class, "evaluateContinuous", span -> {
                    var results = new EvaluatorResults();
                    evaluators.forEach(evaluator -> executeEvaluator(evaluator, realm, knownUser, 1, results));
                    var risk = getRiskScoreAlgorithm(realm).evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.CONTINUOUS, realm, knownUser);

                    if (risk.isValid()) {
                        if (span.isRecording()) {
                            span.setAttribute("keycloak.risk.engine.overall", risk.getScore());
                            span.setAttribute("keycloak.risk.engine.phase", RiskEvaluator.EvaluationPhase.CONTINUOUS.name());
                        }

                        if (risk.getScore() >= RISK_THRESHOLD_LOG_OUT_USER) {
                            session.sessions().removeUserSessions(realm, knownUser);
                            logger.warnf("User with ID %s was logged out due to suspicious activity. Evaluated risk score was %f.%s",
                                    knownUser.getId(),
                                    risk.getScore(),
                                    risk.getSummary().orElse(""));
                            auditContinuousRemediation(realm, knownUser, risk, getRiskScoreAlgorithm(realm), results);
                        }
                    }
                    results.logAll();
                    return risk;
                }), "DefaultRiskEngine.evaluateRiskContinuous");
    }

    protected ResultRisk evaluateRiskAuthentication(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        var timeout = getNumberRealmAttribute(realm, EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(realm, EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        var allEvaluators = getRiskEvaluators(phase, realm);
        var localEvaluators = allEvaluators.stream().filter(e -> !e.isRemote()).collect(Collectors.toCollection(LinkedHashSet::new));
        var remoteEvaluators = allEvaluators.stream().filter(RiskEvaluator::isRemote).collect(Collectors.toCollection(LinkedHashSet::new));

        if (!phase.requiresKnownUser) {
            // BEFORE_AUTHN: fire-and-forget — all evaluators run in the background on Keycloak's managed executor
            var executor = session.getProvider(ExecutorsProvider.class).getExecutor("risk-engine");
            CompletableFuture.runAsync(
                    () -> evaluateAll(phase, realm, knownUser, localEvaluators, remoteEvaluators, retries, timeout), executor)
                    .exceptionally(e -> {
                        logger.errorf(e, "BEFORE_AUTHN background evaluation failed");
                        return null;
                    });
            return risk;
        }

        // USER_KNOWN: synchronous — start remote, run local, await remote, calculate risk
        return evaluateAll(phase, realm, knownUser, localEvaluators, remoteEvaluators, retries, timeout);
    }

    protected ResultRisk evaluateAll(
            @Nonnull RiskEvaluator.EvaluationPhase phase,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            @Nonnull Set<RiskEvaluator> localEvaluators,
            @Nonnull Set<RiskEvaluator> remoteEvaluators,
            int retries,
            @Nonnull Duration timeout
    ) {
        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            return tracingProvider.trace(RiskEngine.class, "evaluateAll", span -> {
                if (span.isRecording()) {
                    span.setAttribute("keycloak.risk.engine.provider", getClass().getSimpleName());
                }

                if (remoteExecutor != null && remoteExecutor.requiresPreInitUserContexts()) {
                    initUserContexts(phase, realm, knownUser);
                }

                var results = new EvaluatorResults();

                // Start remote evaluators first (non-blocking) — they run in parallel while local evaluators execute
                var remoteEvaluation = !remoteEvaluators.isEmpty()
                        ? startRemoteEvaluators(remoteEvaluators, realm, knownUser, retries, timeout, results)
                        : null;

                // Run local evaluators sequentially on the current thread
                var evaluatedRisks = new LinkedHashSet<>(evaluateLocalEvaluators(localEvaluators, realm, knownUser, retries, results));

                // Await remote results
                if (remoteEvaluation != null) {
                    evaluatedRisks.addAll(remoteEvaluation.awaitResults());
                }

                results.logAll();

                var algorithm = getRiskScoreAlgorithm(realm);
                risk = algorithm.evaluateRisk(evaluatedRisks, phase, realm, knownUser);

                if (risk.isValid()) {
                    logger.debugf("The phase risk score is %f - (evaluation phase: %s, algorithm: %s)", risk.getScore(), phase, algorithm.getClass().getSimpleName());

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.phase.score", risk.getScore());
                        span.setAttribute("keycloak.risk.engine.phase", phase.name());
                    }
                }

                ResultRisk overallRisk = null;
                if (phase == RiskEvaluator.EvaluationPhase.USER_KNOWN) {
                    overallRisk = algorithm.getOverallRisk();
                    logger.debugf("The overall risk score is '%f' (algorithm: %s)", overallRisk.getScore(), algorithm.getClass().getSimpleName());
                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.overall", overallRisk.getScore());
                    }
                }

                storePhaseRiskAndAudit(phase, realm, knownUser, risk, overallRisk, algorithm, results);
                return risk;
            });
        }, "DefaultRiskEngine.evaluateAll");
    }

    // --------------------------------------------------
    // Local / remote evaluator execution
    // --------------------------------------------------

    protected Set<RiskEvaluator> evaluateLocalEvaluators(
            @Nonnull Set<RiskEvaluator> evaluators,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            int retries,
            @Nonnull EvaluatorResults results
    ) {
        for (var evaluator : evaluators) {
            tracingProvider.trace(evaluator.getClass(), "evaluate", span -> {
                executeEvaluator(evaluator, realm, knownUser, retries, results);

                if (span.isRecording()) {
                    span.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRisk().getScore().name());
                    evaluator.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                    span.setAttribute("keycloak.risk.engine.evaluator.trust", evaluator.getTrust(realm));
                }
            });
        }

        return evaluators.stream()
                .filter(e -> e.getRisk().isValid())
                .collect(Collectors.toSet());
    }

    @Nullable
    protected RemoteEvaluatorExecutor.RemoteEvaluation startRemoteEvaluators(
            @Nonnull Set<RiskEvaluator> evaluators,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            int retries,
            @Nonnull Duration timeout,
            @Nonnull EvaluatorResults results
    ) {
        if (remoteExecutor == null) {
            logger.warnf("No RemoteEvaluatorExecutor provider available — skipping %d remote evaluator(s): %s",
                    evaluators.size(),
                    evaluators.stream().map(e -> e.getClass().getSimpleName()).collect(Collectors.joining(", ")));
            return null;
        }

        var parentContext = Context.current();

        RemoteEvaluationContext.EvaluatorCallback callback = (evaluator, evalRealm, evalUser) -> {
            try (var ignored = parentContext.makeCurrent()) {
                tracingProvider.trace(evaluator.getClass(), "evaluate", span -> {
                    executeEvaluator(evaluator, evalRealm, evalUser, retries, results);

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRisk().getScore().name());
                        evaluator.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                        span.setAttribute("keycloak.risk.engine.evaluator.trust", evaluator.getTrust(evalRealm));
                    }
                });
            }
        };

        var context = new RemoteEvaluationContext(evaluators, realm, knownUser, timeout, session, callback);
        return remoteExecutor.start(context);
    }

    // --------------------------------------------------
    // Context initialization
    // --------------------------------------------------

    protected void initUserContexts(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        userContexts.stream()
                .filter(f -> f.requiresUser() == phase.requiresKnownUser)
                .filter(f -> !f.isInitialized())
                .filter(f -> !f.isRemote())
                .forEach(f -> f.initData(realm, knownUser));
    }

    // --------------------------------------------------
    // Shared helpers
    // --------------------------------------------------

    protected void executeEvaluator(@Nonnull RiskEvaluator evaluator, @Nonnull RealmModel realm, @Nullable UserModel knownUser, int retries, @Nullable EvaluatorResults results) {
        var startTime = Time.currentTimeMillis();
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
            var duration = Time.currentTimeMillis() - startTime;
            if (results != null) {
                results.add(new EvaluatorResult(
                        evaluator.getClass().getSimpleName(),
                        evaluator.getRisk(),
                        evaluator.getTrust(realm),
                        duration,
                        evaluator.isRemote()
                ));
            }
        }
    }

    protected void storePhaseRiskAndAudit(
            @Nonnull RiskEvaluator.EvaluationPhase phase,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            @Nonnull ResultRisk phaseRisk,
            @Nullable ResultRisk overallRisk,
            @Nonnull RiskScoreAlgorithm algorithm,
            @Nonnull EvaluatorResults results
    ) {
        if (phaseRisk.isValid()) {
            storedRiskProvider.storeRisk(phaseRisk, phase);
        }
        if (overallRisk != null && overallRisk.isValid()) {
            storedRiskProvider.storeOverallRisk(overallRisk);
        }
        if (phase == RiskEvaluator.EvaluationPhase.BEFORE_AUTHN) {
            auditPublisher.stageBeforeAuthnEvaluators(results.snapshot());
        } else if (phase == RiskEvaluator.EvaluationPhase.USER_KNOWN && knownUser != null) {
            auditPublisher.recordLoginEvaluation(realm, knownUser, phaseRisk, overallRisk, algorithm, results.snapshot());
            auditPublisher.flushNow();
        }
    }

    protected void auditContinuousRemediation(
            @Nonnull RealmModel realm,
            @Nonnull UserModel knownUser,
            @Nonnull ResultRisk continuousRisk,
            @Nonnull RiskScoreAlgorithm algorithm,
            @Nonnull EvaluatorResults results
    ) {
        auditPublisher.recordContinuousSessionRevocation(realm, knownUser, continuousRisk, algorithm, results.snapshot());
        auditPublisher.flushNow();
    }

    protected static Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> initializeRiskEvaluators(KeycloakSession session) {
        Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators = new EnumMap<>(RiskEvaluator.EvaluationPhase.class);
        for (RiskEvaluator.EvaluationPhase phase : RiskEvaluator.EvaluationPhase.values()) {
            riskEvaluators.put(phase, new LinkedHashSet<>());
        }

        session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(RiskEvaluator.class)
                .map(RiskEvaluatorFactory.class::cast)
                .forEach(factory -> {
                    var evaluator = session.getProvider(RiskEvaluator.class, factory.getId());
                    if (evaluator != null) {
                        riskEvaluators.get(factory.evaluationPhase()).add(evaluator);
                    }
                });

        return riskEvaluators;
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(RealmModel realm, String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    // --------------------------------------------------
    // Result tracking
    // --------------------------------------------------

    protected record EvaluatorResult(String evaluatorName, Risk risk, double trust, long durationMs, boolean remote) {
        public String format() {
            var score = risk.getScore() != null ? risk.getScore().name() : "N/A";
            String base = String.format("Evaluator: %s [%s] - Risk score: '%s' (trust '%.2f') - %d ms",
                    evaluatorName, remote ? "remote" : "local", score, trust, durationMs);

            return risk.getReason()
                    .filter(r -> !r.isEmpty())
                    .map(reason -> base + " - Reason: " + reason)
                    .orElse(base);
        }
    }

    protected static class EvaluatorResults {
        private final List<EvaluatorResult> results = new CopyOnWriteArrayList<>();

        public void add(EvaluatorResult result) {
            results.add(result);
        }

        public void logAll() {
            results.forEach(r -> logger.debug(r.format()));
        }

        public List<EvaluatorResult> snapshot() {
            return List.copyOf(results);
        }
    }
}
