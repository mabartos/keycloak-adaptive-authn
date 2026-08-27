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
    protected final Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators;
    protected final Set<UserContext> userContexts;
    protected final StoredRiskProvider storedRiskProvider;
    @Nullable
    protected final RemoteEvaluatorExecutor remoteExecutor;

    protected ResultRisk risk = ResultRisk.invalid();
    protected ResultRisk overallRisk = ResultRisk.invalid();

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.userContexts = session.getAllProviders(UserContext.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
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
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping.", phase.name(), storedRisk.getScore());
            return storedRisk;
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
        return overallRisk;
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
        return resolveRiskScoreAlgorithm(session, realm);
    }

    protected static RiskScoreAlgorithm resolveRiskScoreAlgorithm(@Nonnull KeycloakSession session, @Nonnull RealmModel realm) {
        var realmAlgorithm = realm.getAttribute(RISK_SCORE_ALGORITHM_CONFIG);
        if (realmAlgorithm != null) {
            var provider = session.getProvider(RiskScoreAlgorithm.class, realmAlgorithm);
            if (provider != null) {
                return provider;
            }
        }
        return session.getProvider(RiskScoreAlgorithm.class);
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
        var realmId = realm.getId();
        var userId = knownUser.getId();

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            var freshRealm = s.realms().getRealm(realmId);
            var freshUser = s.users().getUserById(freshRealm, userId);
            var evaluators = getEnabledEvaluators(s, RiskEvaluator.EvaluationPhase.CONTINUOUS, freshRealm);

            var outcome = evaluatePhase(s, RiskEvaluator.EvaluationPhase.CONTINUOUS, freshRealm, freshUser, evaluators, 1, DEFAULT_EVALUATOR_TIMEOUT);

            if (outcome.risk.isValid() && outcome.risk.getScore() >= RISK_THRESHOLD_LOG_OUT_USER) {
                s.sessions().removeUserSessions(freshRealm, freshUser);
                logger.warnf("User with ID %s was logged out due to suspicious activity. Evaluated risk score was %f.%s",
                        freshUser.getId(), outcome.risk.getScore(), outcome.risk.getSummary().orElse(""));
                auditContinuousRemediation(s, freshRealm, freshUser, outcome.risk, outcome.algorithm, outcome.results);
            }
            return outcome.risk;
        }, "DefaultRiskEngine.evaluateRiskContinuous");
    }

    protected ResultRisk evaluateRiskAuthentication(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        var retries = getNumberRealmAttribute(realm, EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);
        var timeout = getNumberRealmAttribute(realm, EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);

        if (!phase.requiresKnownUser) {
            evaluateBeforeAuthnInBackground(realm, retries, timeout);
            return ResultRisk.invalid();
        }

        // USER_KNOWN: synchronous — start remote, run local, await remote, calculate risk
        return evaluateAll(session, phase, realm, knownUser, getRiskEvaluators(phase, realm), retries, timeout);
    }

    /**
     * Fire-and-forget BEFORE_AUTHN evaluation on a background thread.
     * <p>
     * Creates a fresh {@link KeycloakSession} with fresh evaluators, then delegates to
     * {@link #evaluateAll} with all evaluators as local (no remote executor needed since
     * we're already off the request thread).
     * <p>
     * TODO: BEFORE_AUTHN and USER_KNOWN execute in different HTTP requests. If this background
     * task hasn't completed by the time USER_KNOWN computes the overall risk, BEFORE_AUTHN
     * evidence will be missing. A coordination mechanism via the auth session (e.g., a completion
     * flag that USER_KNOWN polls with a short timeout) would address this race.
     */
    protected void evaluateBeforeAuthnInBackground(@Nonnull RealmModel realm, int retries, @Nonnull Duration timeout) {
        var sessionFactory = session.getKeycloakSessionFactory();
        var sessionContext = session.getContext();
        var realmId = realm.getId();
        var backgroundExecutor = session.getProvider(ExecutorsProvider.class).getExecutor("risk-engine");

        CompletableFuture.runAsync(() ->
            KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, sessionContext, s -> {
                var freshRealm = s.realms().getRealm(realmId);
                var phase = RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;

                var evaluators = getEnabledEvaluators(s, phase, freshRealm);
                if (evaluators.isEmpty()) {
                    return null;
                }

                return evaluateAll(s, phase, freshRealm, null, evaluators, retries, timeout);
            }, "DefaultRiskEngine.evaluateBeforeAuthnBackground"),
            backgroundExecutor
        ).exceptionally(e -> {
            logger.errorf(e, "BEFORE_AUTHN background evaluation failed");
            return null;
        });
    }

    /**
     * Evaluates risk for BEFORE_AUTHN and USER_KNOWN phases: runs the shared evaluation,
     * computes overall risk (USER_KNOWN only), then stores the result and publishes audit events.
     */
    protected ResultRisk evaluateAll(
            @Nonnull KeycloakSession session,
            @Nonnull RiskEvaluator.EvaluationPhase phase,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            @Nonnull Set<RiskEvaluator> evaluators,
            int retries,
            @Nonnull Duration timeout
    ) {
        var outcome = evaluatePhase(session, phase, realm, knownUser, evaluators, retries, timeout);

        ResultRisk computedOverallRisk = null;
        if (phase == RiskEvaluator.EvaluationPhase.USER_KNOWN) {
            this.risk = outcome.risk;
            computedOverallRisk = outcome.algorithm.getOverallRisk();
            this.overallRisk = computedOverallRisk;
            logger.debugf("The overall risk score is '%f' (algorithm: %s)", computedOverallRisk.getScore(), outcome.algorithm.getClass().getSimpleName());
        }

        storePhaseRiskAndAudit(session, phase, realm, knownUser, outcome.risk, computedOverallRisk, outcome.algorithm, outcome.results);
        return outcome.risk;
    }

    /**
     * Core evaluation logic shared by all phases — splits evaluators, runs them, computes the phase risk.
     * <p>
     * Uses the provided {@code session} for all provider lookups. Callers are responsible for
     * transaction management: the request-scoped session for USER_KNOWN, a fresh session for
     * BEFORE_AUTHN (background thread) and CONTINUOUS (timer).
     * <p>
     * Only USER_KNOWN splits evaluators by {@link RiskEvaluator#isRemote()} — other phases
     * already run off the request thread and treat all evaluators as local.
     */
    protected EvaluationOutcome evaluatePhase(
            @Nonnull KeycloakSession session,
            @Nonnull RiskEvaluator.EvaluationPhase phase,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            @Nonnull Set<RiskEvaluator> evaluators,
            int retries,
            @Nonnull Duration timeout
    ) {
        var tracingProvider = TracingProviderUtil.getTracingProvider(session);
        return tracingProvider.trace(RiskEngine.class, "evaluatePhase", span -> {
            if (span.isRecording()) {
                span.setAttribute("keycloak.risk.engine.provider", getClass().getSimpleName());
            }

            // Only USER_KNOWN dispatches remote evaluators via RemoteEvaluatorExecutor —
            // BEFORE_AUTHN and CONTINUOUS already run off the request thread.
            var localEvaluators = new LinkedHashSet<RiskEvaluator>();
            var remoteEvaluators = new LinkedHashSet<RiskEvaluator>();
            if (phase == RiskEvaluator.EvaluationPhase.USER_KNOWN) {
                for (var e : evaluators) {
                    (e.isRemote() ? remoteEvaluators : localEvaluators).add(e);
                }
            } else {
                localEvaluators.addAll(evaluators);
            }

            // Pre-initialize local user contexts when remote evaluators need them warmed up
            // before running on separate threads. Otherwise contexts are initialized on-demand.
            if (!remoteEvaluators.isEmpty() && remoteExecutor != null && remoteExecutor.requiresPreInitUserContexts()) {
                initUserContexts(phase, realm, knownUser);
            }

            var results = new EvaluatorResults();

            var remoteEvaluation = !remoteEvaluators.isEmpty()
                    ? startRemoteEvaluators(session, tracingProvider, remoteEvaluators, realm, knownUser, retries, timeout, results)
                    : null;

            var evaluatedRisks = new LinkedHashSet<>(evaluateLocalEvaluators(tracingProvider, localEvaluators, realm, knownUser, retries, results));

            if (remoteEvaluation != null) {
                evaluatedRisks.addAll(remoteEvaluation.awaitResults());
            }

            results.logAll();

            var algorithm = resolveRiskScoreAlgorithm(session, realm);
            var phaseRisk = algorithm.evaluateRisk(evaluatedRisks, phase, realm, knownUser);

            if (phaseRisk.isValid()) {
                logger.debugf("The phase risk score is %f - (evaluation phase: %s, algorithm: %s)", phaseRisk.getScore(), phase, algorithm.getClass().getSimpleName());
                if (span.isRecording()) {
                    span.setAttribute("keycloak.risk.engine.phase.score", phaseRisk.getScore());
                    span.setAttribute("keycloak.risk.engine.phase", phase.name());
                }
            }

            return new EvaluationOutcome(phaseRisk, algorithm, results);
        });
    }

    // --------------------------------------------------
    // Local / remote evaluator execution
    // --------------------------------------------------

    protected Set<RiskEvaluator> evaluateLocalEvaluators(
            @Nonnull TracingProvider tracingProvider,
            @Nonnull Set<RiskEvaluator> evaluators,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            int retries,
            @Nonnull EvaluatorResults results
    ) {
        for (var evaluator : evaluators) {
            traceEvaluator(tracingProvider, evaluator, realm, knownUser, retries, results);
        }

        return evaluators.stream()
                .filter(e -> e.getRisk().isValid())
                .collect(Collectors.toSet());
    }

    @Nullable
    protected RemoteEvaluatorExecutor.RemoteEvaluation startRemoteEvaluators(
            @Nonnull KeycloakSession session,
            @Nonnull TracingProvider tracingProvider,
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
                traceEvaluator(tracingProvider, evaluator, evalRealm, evalUser, retries, results);
            }
        };

        var context = new RemoteEvaluationContext(evaluators, realm, knownUser, timeout, session, callback);
        return remoteExecutor.start(context);
    }

    protected void traceEvaluator(@Nonnull TracingProvider tracingProvider, @Nonnull RiskEvaluator evaluator,
            @Nonnull RealmModel realm, @Nullable UserModel knownUser, int retries, @Nonnull EvaluatorResults results) {
        tracingProvider.trace(evaluator.getClass(), "evaluate", span -> {
            executeEvaluator(evaluator, realm, knownUser, retries, results);

            if (span.isRecording()) {
                span.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRisk().getScore().name());
                evaluator.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                span.setAttribute("keycloak.risk.engine.evaluator.trust", evaluator.getTrust(realm));
            }
        });
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
            @Nonnull KeycloakSession session,
            @Nonnull RiskEvaluator.EvaluationPhase phase,
            @Nonnull RealmModel realm,
            @Nullable UserModel knownUser,
            @Nonnull ResultRisk phaseRisk,
            @Nullable ResultRisk overallRisk,
            @Nonnull RiskScoreAlgorithm algorithm,
            @Nonnull EvaluatorResults results
    ) {
        var storedRiskProvider = session.getProvider(StoredRiskProvider.class);
        var auditPublisher = RiskEvaluationAuditPublisher.forSession(session);

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
            @Nonnull KeycloakSession session,
            @Nonnull RealmModel realm,
            @Nonnull UserModel knownUser,
            @Nonnull ResultRisk continuousRisk,
            @Nonnull RiskScoreAlgorithm algorithm,
            @Nonnull EvaluatorResults results
    ) {
        var auditPublisher = RiskEvaluationAuditPublisher.forSession(session);
        auditPublisher.recordContinuousSessionRevocation(realm, knownUser, continuousRisk, algorithm, results.snapshot());
        auditPublisher.flushNow();
    }

    protected static Set<RiskEvaluator> getEnabledEvaluators(KeycloakSession session, RiskEvaluator.EvaluationPhase phase, RealmModel realm) {
        return initializeRiskEvaluators(session).get(phase)
                .stream()
                .filter(e -> e.isEnabled(realm))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    protected record EvaluationOutcome(ResultRisk risk, RiskScoreAlgorithm algorithm, EvaluatorResults results) {}

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
