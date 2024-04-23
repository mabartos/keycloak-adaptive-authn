package org.keycloak.adaptive.policy.basic;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.services.AuthnPolicyConditionResource;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Custom authenticator for evaluating authn policies - for basic usage - use AdvancedAuthnPolicyAuthenticator
public class BasicAuthnPolicyAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResource.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();

        final var provider = session.getProvider(AuthnPolicyProvider.class);
        if (provider == null) {
            throw new IllegalStateException("Cannot find AuthnPolicyProvider");
        }

        var requiresUser = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(BasicAuthnPolicyAuthenticatorFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot find authenticator config '%s'", BasicAuthnPolicyAuthenticatorFactory.REQUIRES_USER_CONFIG)));

        var authPolicies = provider.getAllStream(requiresUser).collect(Collectors.toSet());

        for (var policy : authPolicies) {
            logger.debugf("processing authn policy '%s'", policy.getAlias());

            Map<AuthenticationExecutionModel, ConditionalAuthenticator> conditions = new HashMap<>();
            Map<AuthenticationExecutionModel, Authenticator> actions = new HashMap<>();

            realm.getAuthenticationExecutionsStream(policy.getId())
                    .filter(Objects::nonNull)
                    .filter(f -> !f.isDisabled())
                    .flatMap(f -> f.isAuthenticatorFlow() ? realm.getAuthenticationExecutionsStream(f.getFlowId()) : Stream.of(f))
                    .forEach(f -> {
                        var authFactory = context.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, f.getAuthenticator());
                        if (authFactory != null) {
                            var authenticator = authFactory.create(session);
                            if (authenticator instanceof ConditionalAuthenticator conditionalAuthenticator) {
                                conditions.put(f, conditionalAuthenticator);
                            } else {
                                actions.put(f, authenticator);
                            }
                        }
                    });

            var allConditionsMatch = conditions.entrySet()
                    .stream()
                    .allMatch((entry) -> {
                        var finalContext = new AuthenticationFlowContextWrapper(realm, context, entry.getKey().getAuthenticatorConfig()); // TODO not very good
                        return entry.getValue().matchCondition(finalContext);
                    });

            if (allConditionsMatch) {
                actions.values().forEach(f -> f.authenticate(context));
            } else {
                logger.debugf("conditions for authn policy '%s' were not met", policy.getAlias());
            }
        }

        if (context.getStatus() == null) {
            context.success();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
