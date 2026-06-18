package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

/**
 * Risk evaluator based on which client the user is authenticating to.
 * <p>
 * Produces a <b>signal</b> — not necessarily a risk. Accessing the admin console is a risk signal
 * that warrants stricter authentication, while accessing the account console is a trust signal
 * that can relax requirements.
 * <p>
 * Default sensitivity for well-known Keycloak clients is built-in.
 * Custom clients can override via the {@value #RISK_SENSITIVITY_ATTRIBUTE} client attribute
 * using any {@link Risk.Score} value (e.g. {@code HIGH}, {@code NEGATIVE_LOW}).
 */
@EvaluationPhase(BEFORE_AUTHN)
public class ClientSensitivityRiskEvaluator extends AbstractRiskEvaluator {

    public static final String RISK_SENSITIVITY_ATTRIBUTE = "adaptive-client-riskSensitivity";

    static final Map<String, Risk.Score> DEFAULT_CLIENT_SENSITIVITY = Map.of(
            Constants.ADMIN_CONSOLE_CLIENT_ID, Risk.Score.HIGH,
            Constants.ADMIN_CLI_CLIENT_ID, Risk.Score.HIGH,
            Constants.REALM_MANAGEMENT_CLIENT_ID, Risk.Score.MEDIUM,
            Constants.BROKER_SERVICE_CLIENT_ID, Risk.Score.MEDIUM,
            Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, Risk.Score.NEGATIVE_LOW,
            Constants.ACCOUNT_CONSOLE_CLIENT_ID, Risk.Score.NEGATIVE_LOW
    );

    private final KeycloakSession session;

    public ClientSensitivityRiskEvaluator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        var authSession = session.getContext().getAuthenticationSession();
        if (authSession == null) {
            return Risk.invalid("No authentication session available");
        }

        ClientModel client = authSession.getClient();
        if (client == null) {
            return Risk.invalid("No client in authentication session");
        }

        String clientId = client.getClientId();
        Risk.Score score = resolveScore(client, clientId);

        return Risk.of(score, "Client '%s' sensitivity: %s".formatted(clientId, score.name()));
    }

    private Risk.Score resolveScore(ClientModel client, String clientId) {
        // Explicit attribute takes precedence
        String attr = client.getAttribute(RISK_SENSITIVITY_ATTRIBUTE);
        if (attr != null && !attr.isBlank()) {
            try {
                return Risk.Score.valueOf(attr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall through to defaults
            }
        }

        // Well-known Keycloak default clients
        return DEFAULT_CLIENT_SENSITIVITY.getOrDefault(clientId, Risk.Score.NONE);
    }
}
