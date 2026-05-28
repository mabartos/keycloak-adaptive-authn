package io.github.mabartos.spi.audit;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Session-scoped publisher for risk evaluation audit events.
 * <p>
 * The active instance is stored in the {@link KeycloakSession} under {@link #SESSION_KEY}
 * and shared between the risk engine and authenticators.
 */
public interface RiskAuditPublisher {

    String SESSION_KEY = "adaptive.risk.evaluation.audit.publisher";

    /**
     * Emits an audit event for a login evaluation that was already stored (phase skipped).
     * Call {@link #flushNow()} afterwards to persist the queued event.
     */
    void recordLoginEvaluationFromStored(RealmModel realm, UserModel user);

    /**
     * Persists all queued audit events immediately.
     */
    void flushNow();

    /**
     * Returns the publisher bound to the given session, or {@code null} if the risk engine
     * has not yet initialised one for this request.
     */
    static RiskAuditPublisher get(KeycloakSession session) {
        return session.getAttribute(SESSION_KEY, RiskAuditPublisher.class);
    }
}
