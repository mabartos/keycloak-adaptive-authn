package org.keycloak.adaptive.policy;

import org.keycloak.adaptive.models.AuthnPolicyModel;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;
import java.util.stream.Stream;

public class DefaultAuthnPolicyProvider implements AuthnPolicyProvider {
    private final KeycloakSession session;
    private final RealmModel realm;

    private static final String POLICY_PREFIX = "POLICY - "; // TODO better approach to mark authn policy

    public DefaultAuthnPolicyProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
    }

    @Override
    public AuthnPolicyModel addPolicy(AuthnPolicyModel policy) {
        if (StringUtil.isBlank(policy.getAlias()))
            throw new IllegalArgumentException("Cannot create an authentication policy with an empty alias");

        if (policy.getAlias().startsWith(POLICY_PREFIX)) {
            policy.setAlias(POLICY_PREFIX + policy.getAlias());
        }

        return (AuthnPolicyModel) realm.addAuthenticationFlow(policy); // TODO just cast it for now
    }

    @Override
    public Stream<AuthnPolicyModel> getAllStream() {
        return realm.getAuthenticationFlowsStream()
                .filter(f -> f.getAlias().startsWith(POLICY_PREFIX))
                .map(f -> (AuthnPolicyModel) f);
    }

    @Override
    public Stream<AuthnPolicyModel> getAllStream(boolean requiresUser) {
        return getAllStream().filter(f -> realm.getAuthenticationExecutionsStream(f.getId())
                .flatMap(g -> {
                    if (g.isAuthenticatorFlow()) {
                        var execs = realm.getAuthenticationExecutionsStream(g.getFlowId()); //TODO multiple layers?
                        return execs.map(h -> getAuthenticator(session, h.getAuthenticator()));
                    } else {
                        return Stream.of(getAuthenticator(session, g.getAuthenticator()));
                    }
                }).anyMatch(s -> s.requiresUser() == requiresUser));
    }

    private Authenticator getAuthenticator(KeycloakSession session, String authenticator) {
        return session.getProvider(Authenticator.class, authenticator);
    }

    @Override
    public AuthnPolicyModel getById(String id) {
        return (AuthnPolicyModel) Optional.ofNullable(realm.getAuthenticationFlowById(id))
                .filter(f -> f.getAlias().startsWith(POLICY_PREFIX))
                .orElse(null);
    }

    @Override
    public boolean remove(AuthnPolicyModel policy) {
        if (!policy.getAlias().startsWith(POLICY_PREFIX)) {
            return false;
        }
        realm.removeAuthenticationFlow(policy);
        return true;
    }

    @Override
    public void removeAll() {
        getAllStream().forEach(this::remove); // TODO not very good
    }

    @Override
    public void close() {

    }
}
