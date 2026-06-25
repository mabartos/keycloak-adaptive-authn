package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Risk evaluator for the OAuth client's roles at login
 * ({@link io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase#USER_KNOWN}).
 * <p>
 * Scores the user's roles on the login OAuth client when at least one client role defines
 * {@link #RISK_SCORE_ATTRIBUTE}. Role-name heuristics are not used.
 * Configure per role via Admin Console: Clients → Roles → {role} → Attributes, or realm import / REST.
 * <ul>
 *   <li><strong>No active role attributes</strong> — returns {@link Risk.Score#INVALID} (no contribution).</li>
 *   <li><strong>Active role attributes</strong> — {@link Risk#max(Risk)} over assigned roles with a scorable attribute;
 *       roles without attribute or with explicit {@link Risk.Score#NONE} are ignored (WARN for unconfigured).
 *       If none of the assigned roles contribute a score, returns {@link Risk.Score#INVALID}.</li>
 *   <li><strong>Attributes set but unusable at runtime</strong> — returns {@link Risk.Score#INVALID} (WARN).</li>
 *   <li>Missing attribute on a role — role ignored (WARN when assigned).</li>
 *   <li>Explicit {@link Risk.Score#NONE} — intentional neutral, ignored for scoring.</li>
 * </ul>
 * The risk engine combines enabled evaluators according to realm <strong>Risk-based policies</strong>.
 * Users with no client roles on the target client receive {@link Risk.Score#NEGATIVE_LOW} when role attributes are active.
 */
@EvaluationPhase(USER_KNOWN)
public class ClientRoleRiskEvaluator extends AbstractRiskEvaluator {

    public static final String RISK_SCORE_ATTRIBUTE = "adaptive-client-role-riskScore";

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

        AttributeState attributes = getAttributeState(client);
        if (!attributes.active()) {
            if (attributes.unusable()) {
                LOG.warnf(
                        "ClientRoleRiskEvaluator skipped for client '%s': role risk attributes are set but could not be loaded",
                        clientId);
                return Risk.invalid(
                        "Client role risk attributes are configured but could not be loaded for OAuth client '%s'"
                                .formatted(clientId));
            }
            return Risk.invalid(
                    "No active client role risk attributes for OAuth client '%s'".formatted(clientId));
        }

        Map<String, Optional<Risk.Score>> assignedRoles = new LinkedHashMap<>();
        knownUser.getClientRoleMappingsStream(client).forEach(role ->
                assignedRoles.put(role.getName(), scoreFromRole(role)));

        return evaluateAssignments(clientId, assignedRoles);
    }

    /**
     * Scores assigned client roles. Keys are assigned role names; values are parsed role scores
     * ({@link Optional#empty()} when the attribute is absent).
     */
    static Risk evaluateAssignments(String clientId, Map<String, Optional<Risk.Score>> assignedRoles) {
        if (assignedRoles == null || assignedRoles.isEmpty()) {
            return Risk.of(
                    Risk.Score.NEGATIVE_LOW,
                    "User has no client roles on '%s'".formatted(clientId));
        }

        List<String> unconfiguredRoles = new ArrayList<>();
        List<String> neutralRoles = new ArrayList<>();
        List<String> scoredRoles = new ArrayList<>();

        for (Map.Entry<String, Optional<Risk.Score>> entry : assignedRoles.entrySet()) {
            String roleName = entry.getKey();
            Optional<Risk.Score> configured = entry.getValue();
            if (configured.isEmpty()) {
                unconfiguredRoles.add(roleName);
                continue;
            }
            Risk.Score score = configured.get();
            if (score == Risk.Score.NONE) {
                neutralRoles.add(roleName);
                continue;
            }
            scoredRoles.add(roleName);
        }

        if (scoredRoles.isEmpty()) {
            List<String> ignored = new ArrayList<>(unconfiguredRoles);
            ignored.addAll(neutralRoles);
            LOG.warnf(
                    "ClientRoleRiskEvaluator skipped for client '%s': no assigned role has a scorable '%s' "
                            + "(ignored: %s)",
                    clientId,
                    RISK_SCORE_ATTRIBUTE,
                    ignored);
            return Risk.invalid(
                    "No assigned client role has a scorable %s for OAuth client '%s' (ignored: %s)"
                            .formatted(
                                    RISK_SCORE_ATTRIBUTE,
                                    clientId,
                                    String.join(", ", ignored)));
        }

        if (!unconfiguredRoles.isEmpty()) {
            LOG.warnf(
                    "ClientRoleRiskEvaluator for client '%s': assigned role(s) without '%s' "
                            + "(ignored, scoring configured roles only): %s",
                    clientId,
                    RISK_SCORE_ATTRIBUTE,
                    unconfiguredRoles);
        }

        Risk highest = Risk.of(
                Risk.Score.NEGATIVE_LOW,
                "User has no sensitive client roles on '%s'".formatted(clientId));

        for (String roleName : scoredRoles) {
            Risk.Score score = assignedRoles.get(roleName).orElseThrow();
            Risk current = Risk.of(score, "Client role '%s' on '%s'".formatted(roleName, clientId));
            highest = highest.max(current);
        }

        return highest;
    }

    /**
     * Snapshot of {@link #RISK_SCORE_ATTRIBUTE} presence and parseability across client roles.
     */
    record AttributeState(boolean configured, boolean active) {
        static final AttributeState EMPTY = new AttributeState(false, false);

        boolean unusable() {
            return configured && !active;
        }
    }

    static AttributeState getAttributeState(ClientModel client) {
        if (client == null) {
            return AttributeState.EMPTY;
        }
        Stream<RoleModel> roles = client.getRolesStream();
        if (roles == null) {
            return AttributeState.EMPTY;
        }
        Iterator<RoleModel> iterator = roles.iterator();
        if (!iterator.hasNext()) {
            return AttributeState.EMPTY;
        }
        boolean configured = false;
        boolean active = false;
        do {
            RoleModel role = iterator.next();
            if (!configured && roleDefinesRiskScore(role)) {
                configured = true;
            }
            if (!active && scoreFromRole(role).isPresent()) {
                active = true;
            }
        } while (iterator.hasNext() && !(configured && active));
        return new AttributeState(configured, active);
    }

    static Optional<Risk.Score> scoreFromRole(RoleModel role) {
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
                LOG.warnf("Invalid client role risk score for role '%s' on client '%s': score INVALID is not allowed",
                        role.getName(), clientIdForLog(role));
                return Optional.empty();
            }
            return Optional.of(score);
        } catch (IllegalArgumentException ex) {
            LOG.warnf("Invalid client role risk score for role '%s' on client '%s': %s",
                    role.getName(), clientIdForLog(role), ex.getMessage());
            return Optional.empty();
        }
    }

    private static boolean roleDefinesRiskScore(RoleModel role) {
        return !StringUtil.isBlank(role.getFirstAttribute(RISK_SCORE_ATTRIBUTE));
    }

    private static String clientIdForLog(RoleModel role) {
        if (role.getContainer() instanceof ClientModel client) {
            return client.getClientId();
        }
        return "unknown";
    }
}
