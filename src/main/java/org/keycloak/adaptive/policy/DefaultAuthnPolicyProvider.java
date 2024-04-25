package org.keycloak.adaptive.policy;

import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DefaultAuthnPolicyProvider implements AuthnPolicyProvider {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthenticationFlowModel parent;

    private static final String POLICY_PREFIX = "POLICY - "; // TODO better approach to mark authn policy

    public DefaultAuthnPolicyProvider(KeycloakSession session) {
        this(session, session.getContext().getRealm());
    }

    public DefaultAuthnPolicyProvider(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
        // Create parent authn policies flow if does not exist
        this.parent = getOrCreateParentPolicy();
    }

    @Override
    public AuthenticationFlowModel getParentPolicy() {
        return Optional.ofNullable(realm.getFlowByAlias(DefaultAuthnPolicyFactory.DEFAULT_AUTHN_POLICIES_FLOW_ALIAS))
                .orElseThrow(() -> new IllegalStateException(String.format("Authentication policies Parent Flow '%s' does not exist", DefaultAuthnPolicyFactory.DEFAULT_AUTHN_POLICIES_FLOW_ALIAS)));
    }

    @Override
    public AuthenticationFlowModel getOrCreateParentPolicy() {
        return Optional.ofNullable(realm.getFlowByAlias(DefaultAuthnPolicyFactory.DEFAULT_AUTHN_POLICIES_FLOW_ALIAS)).orElseGet(() -> DefaultAuthnPolicyFactory.createParentFlow(realm));
    }

    @Override
    public AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy) {
        return addPolicy(policy, parent.getId());
    }

    @Override
    public AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy, String parentFlowId) {
        if (StringUtil.isBlank(policy.getAlias()))
            throw new IllegalArgumentException("Cannot create an authentication policy with an empty alias");

        if (!policy.getAlias().startsWith(POLICY_PREFIX)) {
            policy.setAlias(POLICY_PREFIX + policy.getAlias());
        }

        var flow = realm.addAuthenticationFlow(policy);

        var execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlowId);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        execution.setPriority(0);
        execution.setFlowId(flow.getId());
        execution.setAuthenticatorFlow(true);

        realm.addAuthenticatorExecution(execution);

        return flow;
    }

    @Override
    public Stream<AuthenticationFlowModel> getAllStream() {
        return realm.getAuthenticationExecutionsStream(parent.getId())
                .filter(AuthenticationExecutionModel::isAuthenticatorFlow)
                .map(f -> realm.getAuthenticationFlowById(f.getFlowId()))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<AuthenticationFlowModel> getAllStream(boolean requiresUser) {
        final Predicate<Stream<Authenticator>> OPERATION = requiresUser ?
                s -> s.anyMatch(Authenticator::requiresUser) :
                s -> s.noneMatch(Authenticator::requiresUser);

        final Predicate<AuthenticationFlowModel> FILTER = f -> OPERATION.test(
                getAllAuthenticationExecutionsStream(f.getId()).map(g -> getAuthenticator(session, g.getAuthenticator()))
        );

        return getAllStream().filter(FILTER);
    }

    private Stream<AuthenticationExecutionModel> getAllAuthenticationExecutionsStream(String flowId) {
        return realm.getAuthenticationExecutionsStream(flowId).flatMap(g -> {
            if (g.isAuthenticatorFlow()) {
                return getAllAuthenticationExecutionsStream(g.getFlowId());
            } else {
                return Stream.of(g);
            }
        });
    }

    private Authenticator getAuthenticator(KeycloakSession session, String authenticator) {
        return session.getProvider(Authenticator.class, authenticator);
    }

    @Override
    public Optional<AuthenticationFlowModel> getById(String id) {
        return getAllStream().filter(f -> f.getId().equals(id)).findAny();
    }

    @Override
    public Optional<AuthenticationFlowModel> getByAlias(String alias) {
        return getAllStream().filter(f -> f.getAlias().equals(alias)).findAny();
    }

    @Override
    public boolean remove(AuthenticationFlowModel policy) {
        var found = getById(policy.getId());
        if (found.isEmpty()) {
            return false;
        }
        realm.removeAuthenticationFlow(found.get());
        return true;
    }

    @Override
    public void removeAll() {
        getAllStream().forEach(this::remove); // TODO not very good
    }

    @Override
    public void update(AuthenticationFlowModel policy) {
        var found = getById(policy.getId());
        if (found.isEmpty()) return;
        realm.updateAuthenticationFlow(policy);
    }

    @Override
    public void close() {

    }
}
