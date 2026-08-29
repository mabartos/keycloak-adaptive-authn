package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;
import java.util.Set;

/**
 * Risk evaluator for the OAuth client's roles at login
 * ({@link io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase#USER_KNOWN}).
 * <p>
 * Uses Keycloak's role naming convention to classify risk by prefix. Per-role attribute
 * {@link #RISK_SCORE_ATTRIBUTE} overrides the prefix score when set.
 * Configure via Admin Console: Clients → Roles → {role} → Attributes, or realm import / REST.
 * <ul>
 *   <li><strong>No attribute</strong> — prefix heuristics apply ({@code manage-*}, {@code create-*}, etc.).</li>
 *   <li><strong>Attribute set</strong> — explicit score takes precedence over prefix heuristics.</li>
 *   <li><strong>Explicit {@link Risk.Score#NONE}</strong> — intentional neutral override.</li>
 *   <li><strong>Invalid attribute</strong> — WARN and fall back to prefix heuristics.</li>
 * </ul>
 * Users with no client roles on the target client receive a trust signal ({@link Risk.Score#NEGATIVE_LOW}).
 */
@EvaluationPhase(USER_KNOWN)
public class ClientRoleRiskEvaluator extends AbstractRiskEvaluator {

    public static final String RISK_SCORE_ATTRIBUTE = "adaptive-client-role-riskScore";

    private static final String MANAGE_PREFIX = "manage-";
    private static final String CREATE_PREFIX = "create-";
    private static final String VIEW_PREFIX = "view-";
    private static final String QUERY_PREFIX = "query-";

    private static final Set<String> SENSITIVE_ROLES = Set.of(AdminRoles.IMPERSONATION);

    private static final Logger LOG = Logger.getLogger(ClientRoleRiskEvaluator.class);

    private final KeycloakSession session;

    public ClientRoleRiskEvaluator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var authSession = session.getContext().getAuthenticationSession();
        if (authSession == null) {
            return Risk.invalid("No authentication session available");
        }

        ClientModel client = authSession.getClient();
        if (client == null) {
            return Risk.invalid("No client in authentication session");
        }

        String clientId = client.getClientId();
        Risk highest = Risk.of(
                Risk.Score.NEGATIVE_LOW,
                "User has no sensitive client roles on '%s'".formatted(clientId));

        for (RoleModel role : knownUser.getClientRoleMappingsStream(client).toList()) {
            Risk.Score score = scoreForRole(role);
            Risk current = Risk.of(score, "Client role '%s' on '%s'".formatted(role.getName(), clientId));
            highest = highest.max(current);
        }

        return highest;
    }

    /**
     * Resolves the effective score for a client role: explicit attribute overrides prefix heuristics.
     */
    private static Risk.Score scoreForRole(RoleModel role) {
        if (role == null) {
            return Risk.Score.NONE;
        }
        return parseAttributeScore(role).orElseGet(() -> scoreFromRoleName(role.getName()));
    }

    /**
     * Parses {@link #RISK_SCORE_ATTRIBUTE} when present. Empty when the attribute is absent;
     * invalid values are logged and yield empty so callers fall back to prefix heuristics.
     */
    private static Optional<Risk.Score> parseAttributeScore(RoleModel role) {
        if (role == null) {
            return Optional.empty();
        }
        String scoreRaw = role.getFirstAttribute(RISK_SCORE_ATTRIBUTE);
        if (StringUtil.isBlank(scoreRaw)) {
            return Optional.empty();
        }
        try {
            Risk.Score score = Risk.Score.valueOf(scoreRaw.trim().toUpperCase());
            if (score == Risk.Score.INVALID) {
                LOG.warnf(
                        "Invalid client role risk score for role '%s' on client '%s': score INVALID is not allowed",
                        role.getName(), clientIdForLog(role));
                return Optional.empty();
            }
            return Optional.of(score);
        } catch (IllegalArgumentException ex) {
            LOG.warnf(
                    "Invalid client role risk score for role '%s' on client '%s': %s",
                    role.getName(), clientIdForLog(role), ex.getMessage());
            return Optional.empty();
        }
    }

    private static Risk.Score scoreFromRoleName(String roleName) {
        if (SENSITIVE_ROLES.contains(roleName)) {
            return Risk.Score.MEDIUM;
        }
        if (roleName.startsWith(MANAGE_PREFIX)) {
            return Risk.Score.MEDIUM;
        }
        if (roleName.startsWith(CREATE_PREFIX)) {
            return Risk.Score.SMALL;
        }
        if (roleName.startsWith(VIEW_PREFIX)) {
            return Risk.Score.NONE;
        }
        if (roleName.startsWith(QUERY_PREFIX)) {
            return Risk.Score.NONE;
        }
        return Risk.Score.NONE;
    }

    private static String clientIdForLog(RoleModel role) {
        if (role.getContainer() instanceof ClientModel client) {
            return client.getClientId();
        }
        return "unknown";
    }
}
