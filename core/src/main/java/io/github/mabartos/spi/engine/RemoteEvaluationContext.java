package io.github.mabartos.spi.engine;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.util.Set;

/**
 * Context passed to {@link RemoteEvaluatorExecutor} containing everything needed to execute remote evaluators.
 *
 * @param evaluators the remote evaluators to execute
 * @param realm      the realm
 * @param knownUser  the user (can be null for BEFORE_AUTHN phase)
 * @param timeout    maximum duration for the remote evaluation
 * @param session    the Keycloak session (for creating transactions, accessing providers)
 * @param callback   callback to execute per evaluator — wraps the engine's executeEvaluator + tracing
 */
public record RemoteEvaluationContext(
        Set<RiskEvaluator> evaluators,
        RealmModel realm,
        @Nullable UserModel knownUser,
        Duration timeout,
        KeycloakSession session,
        EvaluatorCallback callback
) {

    /**
     * Callback for executing a single evaluator. The engine constructs this to wrap
     * {@code executeEvaluator} + tracing span attributes, capturing retries and results via closure.
     * <p>
     * The executor calls this per evaluator without knowing engine internals.
     */
    @FunctionalInterface
    public interface EvaluatorCallback {
        void execute(RiskEvaluator evaluator, RealmModel realm, @Nullable UserModel knownUser);
    }
}
