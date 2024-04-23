package org.keycloak.adaptive.policy.ephemeral;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.EphemeralFlowProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class AuthnPolicyEphemeralProvider implements EphemeralFlowProvider {
    private static final Logger logger = Logger.getLogger(AuthnPolicyEphemeralProvider.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final boolean requiresUser;
    private final int priority;

    public AuthnPolicyEphemeralProvider(KeycloakSession session, boolean requiresUser, int priority) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.requiresUser = requiresUser;
        this.priority = priority;
    }

    @Override
    public Stream<AuthenticationExecutionModel> getExecutions(AuthenticationFlowModel parentFlow) {
        final var browserFlow = realm.getBrowserFlow();
        if (!browserFlow.getId().equals(parentFlow.getId())) {
            return Stream.empty(); // Add only to browser-flow
        }

        final var alias = requiresUser ? AuthnPolicyRequiresUserFactory.ALIAS : AuthnPolicyNoUserFactory.ALIAS;

        final AuthenticationFlowModel flow = realm.getFlowByAlias(alias);
        if (flow == null) {
            throw new IllegalStateException(String.format("Cannot find authentication flow '%s'", alias));
        }

        final var execution = realm.getAuthenticationExecutionByFlowId(flow.getId());
        if (execution == null) {
            throw new IllegalStateException(String.format("Cannot find authentication flow execution '%s'", alias));
        }

        execution.setParentFlow(browserFlow.getId());
        execution.setPriority(priority);

        final var isAnyAlternativeFlow = realm.getAuthenticationExecutionsStream(browserFlow.getId())
                .anyMatch(AuthenticationExecutionModel::isAlternative);

        execution.setRequirement(isAnyAlternativeFlow ?
                AuthenticationExecutionModel.Requirement.ALTERNATIVE :
                AuthenticationExecutionModel.Requirement.REQUIRED);

        final var provider = session.getProvider(AuthnPolicyProvider.class);
        if (provider == null) {
            throw new IllegalStateException("Cannot find AuthnPolicyProvider");
        }

        AtomicInteger offset = new AtomicInteger(10);
        final var authnPolicyExecutions = provider.getAllStream(requiresUser)
                .map(f -> realm.getAuthenticationExecutionsStream(f.getId()).toList())
                .toList();

        authnPolicyExecutions.forEach(f -> {
            logger.debugf("set priority for lists");
            f.forEach(g -> {
                logger.debugf("set priority for execution %s", g.getId());
                g.setParentFlow(flow.getId());
                g.setPriority(priority + offset.get() + g.getPriority());
            });
            offset.addAndGet(10);
        });

        return Stream.concat(authnPolicyExecutions.stream().flatMap(Collection::stream), Stream.of(execution))
                .sorted(AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
    }

    @Override
    public void close() {

    }
}
