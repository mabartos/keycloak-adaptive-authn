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
    }

    @Override
    public AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy) {
        if (StringUtil.isBlank(policy.getAlias()))
            throw new IllegalArgumentException("Cannot create an authentication policy with an empty alias");

        if (policy.getAlias().startsWith(POLICY_PREFIX)) {
            policy.setAlias(POLICY_PREFIX + policy.getAlias());
        }

        return realm.addAuthenticationFlow(policy);
    }

    @Override
    public Stream<AuthenticationFlowModel> getAllStream() {
        return realm.getAuthenticationFlowsStream()
                .filter(Objects::nonNull)
                .filter(f -> f.getAlias().startsWith(POLICY_PREFIX));
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
    public AuthenticationFlowModel getById(String id) {
        return Optional.ofNullable(realm.getAuthenticationFlowById(id))
                .filter(f -> f.getAlias().startsWith(POLICY_PREFIX))
                .orElse(null);
    }

    @Override
    public boolean remove(AuthenticationFlowModel policy) {
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
