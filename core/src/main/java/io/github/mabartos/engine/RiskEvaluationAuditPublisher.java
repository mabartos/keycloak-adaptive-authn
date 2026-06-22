package io.github.mabartos.engine;

import io.github.mabartos.spi.audit.RiskAuditPublisher;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_SCORE_ALGORITHM_CONFIG;
import static io.github.mabartos.engine.algorithm.LogOddsRiskAlgorithmFactory.PROVIDER_ID;

/**
 * Publishes optional user events for login risk evaluation (after {@code USER_KNOWN}) and for
 * {@code CONTINUOUS} automatic remediation when sessions are revoked.
 */
public class RiskEvaluationAuditPublisher implements RiskAuditPublisher {

    private static final Logger logger = Logger.getLogger(RiskEvaluationAuditPublisher.class);

    /**
     * Discriminator in event details (Keycloak has no extension-specific {@link EventType} yet).
     */
    public static final String DETAIL_SUBTYPE = "custom_required_action";
    public static final String SUBTYPE_LOGIN = "adaptive-risk-evaluation";
    public static final String SUBTYPE_REMEDIATION = "adaptive-risk-remediation";

    public static final String DETAIL_PHASE = "adaptive_phase";
    public static final String DETAIL_CONTINUOUS_SCORE = "adaptive_continuous_score";
    public static final String DETAIL_CONTINUOUS_EVALUATORS = "adaptive_continuous_evaluators";
    public static final String DETAIL_REMEDIATION = "adaptive_remediation";
    public static final String DETAIL_SUMMARY = "adaptive_summary";
    public static final String REMEDIATION_SESSIONS_REVOKED = "sessions_revoked";

    public static final String AUTH_NOTE_BEFORE_AUTHN_EVALUATORS = "adaptive.audit.before-authn.evaluators";

    public static final String DETAIL_ALGORITHM = "adaptive_algorithm";
    public static final String DETAIL_BEFORE_AUTHN_SCORE = "adaptive_before_authn_score";
    public static final String DETAIL_BEFORE_AUTHN_LEVEL = "adaptive_before_authn_level";
    public static final String DETAIL_USER_KNOWN_SCORE = "adaptive_user_known_score";
    public static final String DETAIL_USER_KNOWN_LEVEL = "adaptive_user_known_level";
    public static final String DETAIL_OVERALL_SCORE = "adaptive_overall_score";
    public static final String DETAIL_OVERALL_LEVEL = "adaptive_overall_level";
    public static final String DETAIL_CONTINUOUS_LEVEL = "adaptive_continuous_level";
    public static final String DETAIL_BEFORE_AUTHN_EVALUATORS = "adaptive_before_authn_evaluators";
    public static final String DETAIL_USER_KNOWN_EVALUATORS = "adaptive_user_known_evaluators";
    public static final String DETAIL_CLIENT_ID = "adaptive_client_id";

    static final int MAX_EVALUATORS_PER_PHASE = 20;
    static final int MAX_INVALID_REASON_LENGTH = 40;
    /** Evaluators joined with this separator in Keycloak event details UI. */
    static final String EVALUATORS_DETAIL_LINE_SEPARATOR = ", ";

    private final KeycloakSession session;
    private final StoredRiskProvider storedRiskProvider;
    /** Per-request queue; add and {@link #flushNow()} run on the same thread for a given flow. */
    private final List<PendingAuditEvent> pending = new ArrayList<>();

    /**
     * Returns the single audit publisher for this {@link KeycloakSession} (shared by the risk engine and authenticators).
     * Stored under {@link RiskAuditPublisher#SESSION_KEY} so it is also accessible via {@link RiskAuditPublisher#get(KeycloakSession)}.
     */
    @Nonnull
    public static RiskEvaluationAuditPublisher forSession(@Nonnull KeycloakSession session) {
        var existing = session.getAttribute(SESSION_KEY, RiskAuditPublisher.class);
        if (existing instanceof RiskEvaluationAuditPublisher publisher) {
            return publisher;
        }
        var publisher = new RiskEvaluationAuditPublisher(session);
        session.setAttribute(SESSION_KEY, publisher);
        return publisher;
    }

    public RiskEvaluationAuditPublisher(KeycloakSession session) {
        this.session = session;
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    /**
     * Retains per-evaluator results from {@code BEFORE_AUTHN} for the consolidated login audit event.
     */
    public void stageBeforeAuthnEvaluators(@Nonnull List<AbstractRiskEngine.EvaluatorResult> evaluatorResults) {
        if (!RiskEvaluationAuditConfig.isAuditEnabled(session.getContext().getRealm())) {
            return;
        }
        var formatted = formatEvaluators(evaluatorResults);
        if (StringUtil.isNotBlank(formatted)) {
            storedRiskProvider.storeAdditionalData(AUTH_NOTE_BEFORE_AUTHN_EVALUATORS, formatted);
        }
    }

    /**
     * Records a single login audit event after {@code USER_KNOWN} evaluation (call {@link #flushNow()} to persist).
     */
    public void recordLoginEvaluation(
            @Nonnull RealmModel realm,
            @Nonnull UserModel user,
            @Nonnull ResultRisk userKnownRisk,
            @Nullable ResultRisk overallRisk,
            @Nonnull RiskScoreAlgorithm algorithm,
            @Nonnull List<AbstractRiskEngine.EvaluatorResult> userKnownEvaluatorResults
    ) {
        if (!RiskEvaluationAuditConfig.isAuditEnabled(realm)) {
            logger.debugf("Risk evaluation audit disabled for realm %s (events off or %s not in saved event types)",
                    realm.getName(), RiskEvaluationAuditConfig.AUDIT_EVENT_TYPE_NAME);
            return;
        }
        if (!userKnownRisk.isValid() && (overallRisk == null || !overallRisk.isValid())) {
            logger.debugf("Skipping login risk audit for user %s: invalid USER_KNOWN and overall risk", user.getId());
            return;
        }

        var beforeAuthnRisk = storedRiskProvider.getStoredRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        var beforeAuthnEvaluators = storedRiskProvider.getAdditionalData(AUTH_NOTE_BEFORE_AUTHN_EVALUATORS).orElse(null);

        pending.add(buildLoginSnapshot(realm, user, beforeAuthnRisk, userKnownRisk, overallRisk, algorithm, beforeAuthnEvaluators, userKnownEvaluatorResults));
        logger.debugf("Queued login risk audit for user %s (realm %s)", user.getId(), realm.getName());
    }

    /**
     * Records an audit event when {@code CONTINUOUS} evaluation revokes active sessions (call {@link #flushNow()} to persist).
     */
    public void recordContinuousSessionRevocation(
            @Nonnull RealmModel realm,
            @Nonnull UserModel user,
            @Nonnull ResultRisk continuousRisk,
            @Nonnull RiskScoreAlgorithm algorithm,
            @Nonnull List<AbstractRiskEngine.EvaluatorResult> evaluatorResults
    ) {
        if (!RiskEvaluationAuditConfig.isAuditEnabled(realm) || !continuousRisk.isValid()) {
            return;
        }

        pending.add(new RemediationAuditEvent(
                realm,
                user.getId(),
                resolveAlgorithmId(realm),
                formatScore(continuousRisk.getScore()),
                resolveSimpleLevelName(continuousRisk.getScore(), algorithm),
                continuousRisk.getSummary().filter(StringUtil::isNotBlank),
                formatEvaluators(evaluatorResults)
        ));
    }

    /**
     * Persists queued audit events (call from the Mutiny callback once evaluation finished).
     */
    public void flushNow() {
        if (pending.isEmpty()) {
            return;
        }
        var events = List.copyOf(pending);
        pending.clear();

        for (PendingAuditEvent event : events) {
            try {
                switch (event) {
                    case LoginAuditEvent login -> emitLogin(login);
                    case RemediationAuditEvent remediation -> emitRemediation(remediation);
                }
            } catch (RuntimeException e) {
                logger.warnf(e, "Failed to publish risk evaluation audit event for user %s", event.userId());
            }
        }
    }

    private void emitLogin(LoginAuditEvent snapshot) {
        var builder = newEventBuilder(snapshot.realm(), snapshot.userId());
        builder.event(EventType.CUSTOM_REQUIRED_ACTION)
                .detail(DETAIL_SUBTYPE, SUBTYPE_LOGIN)
                .detail(DETAIL_ALGORITHM, snapshot.algorithmId());

        snapshot.clientId().ifPresent(clientId -> builder.detail(DETAIL_CLIENT_ID, clientId));

        snapshot.beforeAuthnScore().ifPresent(score -> builder.detail(DETAIL_BEFORE_AUTHN_SCORE, score));
        snapshot.beforeAuthnLevel().ifPresent(level -> builder.detail(DETAIL_BEFORE_AUTHN_LEVEL, level));
        snapshot.userKnownScore().ifPresent(score -> builder.detail(DETAIL_USER_KNOWN_SCORE, score));
        snapshot.userKnownLevel().ifPresent(level -> builder.detail(DETAIL_USER_KNOWN_LEVEL, level));
        snapshot.overallScore().ifPresent(score -> builder.detail(DETAIL_OVERALL_SCORE, score));
        snapshot.overallLevel().ifPresent(level -> builder.detail(DETAIL_OVERALL_LEVEL, level));
        snapshot.beforeAuthnEvaluators().ifPresent(detail -> builder.detail(DETAIL_BEFORE_AUTHN_EVALUATORS, detail));
        snapshot.userKnownEvaluators().ifPresent(detail -> builder.detail(DETAIL_USER_KNOWN_EVALUATORS, detail));

        builder.storeImmediately(true).success();
    }

    private void emitRemediation(RemediationAuditEvent snapshot) {
        var builder = newEventBuilder(snapshot.realm(), snapshot.userId());
        builder.event(EventType.CUSTOM_REQUIRED_ACTION)
                .detail(DETAIL_SUBTYPE, SUBTYPE_REMEDIATION)
                .detail(DETAIL_PHASE, RiskEvaluator.EvaluationPhase.CONTINUOUS.name())
                .detail(DETAIL_REMEDIATION, REMEDIATION_SESSIONS_REVOKED)
                .detail(DETAIL_ALGORITHM, snapshot.algorithmId())
                .detail(DETAIL_CONTINUOUS_SCORE, snapshot.continuousScore())
                .detail(DETAIL_CONTINUOUS_LEVEL, snapshot.continuousLevel());

        snapshot.summary().ifPresent(summary -> builder.detail(DETAIL_SUMMARY, summary));
        if (StringUtil.isNotBlank(snapshot.evaluators())) {
            builder.detail(DETAIL_CONTINUOUS_EVALUATORS, snapshot.evaluators());
        }

        builder.storeImmediately(true).success();
    }

    /**
     * Emits audit from stored auth notes when evaluation was skipped (phase already computed).
     */
    public void recordLoginEvaluationFromStored(@Nonnull RealmModel realm, @Nonnull UserModel user) {
        if (!RiskEvaluationAuditConfig.isAuditEnabled(realm)) {
            return;
        }
        var userKnownRisk = storedRiskProvider.getStoredRisk(RiskEvaluator.EvaluationPhase.USER_KNOWN);
        var overallRisk = storedRiskProvider.getStoredOverallRisk();
        if (!userKnownRisk.isValid() && !overallRisk.isValid()) {
            logger.debugf("Skipping stored login risk audit for user %s: no valid USER_KNOWN or overall risk", user.getId());
            return;
        }
        recordLoginEvaluation(
                realm,
                user,
                userKnownRisk,
                overallRisk.isValid() ? overallRisk : null,
                session.getProvider(RiskScoreAlgorithm.class),
                List.of()
        );
    }

    private EventBuilder newEventBuilder(RealmModel realm, String userId) {
        EventBuilder builder = resolveClientConnection()
                .map(connection -> new EventBuilder(realm, session, connection))
                .orElseGet(() -> new EventBuilder(realm, session));
        builder.user(userId);
        return builder;
    }

    /**
     * HTTP client connection is only available during request-scoped flows (login).
     * Background tasks (e.g. continuous risk timer) must build events without it.
     */
    private Optional<ClientConnection> resolveClientConnection() {
        try {
            return Optional.ofNullable(session.getContext().getConnection());
        } catch (RuntimeException e) {
            logger.tracef("No request context for audit event IP: %s", e.getMessage());
            return Optional.empty();
        }
    }

    private LoginAuditEvent buildLoginSnapshot(
            RealmModel realm,
            UserModel user,
            ResultRisk beforeAuthnRisk,
            ResultRisk userKnownRisk,
            @Nullable ResultRisk overallRisk,
            RiskScoreAlgorithm algorithm,
            @Nullable String beforeAuthnEvaluators,
            List<AbstractRiskEngine.EvaluatorResult> userKnownEvaluatorResults
    ) {
        var authSession = session.getContext().getAuthenticationSession();
        var clientId = authSession != null && authSession.getClient() != null
                ? authSession.getClient().getClientId()
                : null;
        var userKnownEvaluators = formatEvaluators(userKnownEvaluatorResults);

        return new LoginAuditEvent(
                realm,
                user.getId(),
                resolveAlgorithmId(realm),
                Optional.ofNullable(clientId).filter(StringUtil::isNotBlank),
                scoreAndLevel(beforeAuthnRisk, algorithm),
                scoreAndLevel(userKnownRisk, algorithm),
                overallRisk != null && overallRisk.isValid() ? scoreAndLevel(overallRisk, algorithm) : ScoreAndLevel.empty(),
                Optional.ofNullable(beforeAuthnEvaluators).filter(StringUtil::isNotBlank),
                StringUtil.isNotBlank(userKnownEvaluators) ? Optional.of(userKnownEvaluators) : Optional.empty()
        );
    }

    private static ScoreAndLevel scoreAndLevel(ResultRisk risk, RiskScoreAlgorithm algorithm) {
        if (!risk.isValid()) {
            return ScoreAndLevel.empty();
        }
        var score = risk.getScore();
        return new ScoreAndLevel(
                Optional.of(formatScore(score)),
                Optional.of(resolveSimpleLevelName(score, algorithm))
        );
    }

    private record ScoreAndLevel(Optional<String> score, Optional<String> level) {
        static ScoreAndLevel empty() {
            return new ScoreAndLevel(Optional.empty(), Optional.empty());
        }
    }

    private sealed interface PendingAuditEvent {
        String userId();
    }

    private record LoginAuditEvent(
            RealmModel realm,
            String userId,
            String algorithmId,
            Optional<String> clientId,
            ScoreAndLevel beforeAuthn,
            ScoreAndLevel userKnown,
            ScoreAndLevel overall,
            Optional<String> beforeAuthnEvaluators,
            Optional<String> userKnownEvaluators
    ) implements PendingAuditEvent {
        Optional<String> beforeAuthnScore() {
            return beforeAuthn.score();
        }

        Optional<String> beforeAuthnLevel() {
            return beforeAuthn.level();
        }

        Optional<String> userKnownScore() {
            return userKnown.score();
        }

        Optional<String> userKnownLevel() {
            return userKnown.level();
        }

        Optional<String> overallScore() {
            return overall.score();
        }

        Optional<String> overallLevel() {
            return overall.level();
        }
    }

    private record RemediationAuditEvent(
            RealmModel realm,
            String userId,
            String algorithmId,
            String continuousScore,
            String continuousLevel,
            Optional<String> summary,
            String evaluators
    ) implements PendingAuditEvent {
    }

    static String resolveAlgorithmId(RealmModel realm) {
        var configured = realm.getAttribute(RISK_SCORE_ALGORITHM_CONFIG);
        if (StringUtil.isNotBlank(configured)) {
            return configured;
        }
        return PROVIDER_ID;
    }

    static String formatScore(double score) {
        return String.format(Locale.ROOT, "%.4f", score);
    }

    /**
     * Maps numeric score to simple flow level (LOW / MEDIUM / HIGH), same bands as {@code RiskLevelCondition}.
     */
    static String resolveSimpleLevelName(double score, RiskScoreAlgorithm algorithm) {
        return resolveSimpleLevelName(score, algorithm.getSimpleRiskLevels());
    }

    static String resolveSimpleLevelName(double score, SimpleRiskLevels levels) {
        return levels.getLevels().stream()
                .filter(level -> level.matchesRisk(score))
                .map(RiskLevel::name)
                .findFirst()
                .orElse("UNKNOWN");
    }

    static String formatEvaluators(List<AbstractRiskEngine.EvaluatorResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        return results.stream()
                .sorted(compareEvaluatorsBySeverity())
                .map(RiskEvaluationAuditPublisher::formatEvaluatorEntry)
                .filter(Objects::nonNull)
                .limit(MAX_EVALUATORS_PER_PHASE)
                .collect(Collectors.joining(EVALUATORS_DETAIL_LINE_SEPARATOR));
    }

    /**
     * Highest {@link io.github.mabartos.spi.level.Risk.Score} ordinal first; {@code INVALID} last; name as tie-breaker.
     */
    static Comparator<AbstractRiskEngine.EvaluatorResult> compareEvaluatorsBySeverity() {
        return Comparator
                .comparingInt(RiskEvaluationAuditPublisher::evaluatorSeverityOrdinal)
                .reversed()
                .thenComparing(AbstractRiskEngine.EvaluatorResult::evaluatorName);
    }

    static int evaluatorSeverityOrdinal(AbstractRiskEngine.EvaluatorResult result) {
        var risk = result.risk();
        if (risk == null || risk.getScore() == null) {
            return -1;
        }
        return risk.getScore().ordinal();
    }

    /**
     * One evaluator line: {@code Name=SCORE} or {@code Name=INVALID:reason} (reason truncated, no line breaks / {@code =}).
     */
    @Nullable
    static String formatEvaluatorEntry(AbstractRiskEngine.EvaluatorResult result) {
        var risk = result.risk();
        if (risk == null || risk.getScore() == null) {
            return null;
        }
        var entry = result.evaluatorName() + "=" + risk.getScore().name();
        if (!risk.isValid()) {
            return risk.getReason()
                    .map(reason -> entry + ":" + sanitizeDetailFragment(reason, MAX_INVALID_REASON_LENGTH))
                    .orElse(entry);
        }
        return entry;
    }

    static String sanitizeDetailFragment(String value, int maxLength) {
        if (StringUtil.isBlank(value)) {
            return "";
        }
        var sanitized = value.replace(',', ' ')
                .replace('=', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength);
    }

}
