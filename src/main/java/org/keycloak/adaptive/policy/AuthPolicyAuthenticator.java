package org.keycloak.adaptive.policy;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.services.AuthnPolicyConditionResource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthPolicyAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResource.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var config = context.getAuthenticatorConfig();
        if (config != null) {
            var requiresUser = Optional.ofNullable(config.getConfig().get(AuthPolicyAuthenticatorFactory.REQUIRES_USER_CONFIG)).filter(StringUtil::isNotBlank).filter(f -> f.equals(Boolean.TRUE.toString()) || f.equals(Boolean.FALSE.toString())).map(Boolean::parseBoolean).orElseThrow(() -> new IllegalArgumentException(String.format("Cannot parse '%s' property", AuthPolicyAuthenticatorFactory.REQUIRES_USER_CONFIG)));

            RealmModel realm = context.getRealm();
            KeycloakSession session = context.getSession();
            var authPolicies = realm.getAuthenticationFlowsStream().filter(f -> f.getAlias().startsWith("POLICY -")) // TODO have better approach how to determine it's auth policy flow
                    .collect(Collectors.toSet());

            for (var policy : authPolicies) {
                final Map<AuthenticationExecutionModel, ConditionalAuthenticator> topConditions = new HashMap<>();
                final Map<AuthenticationExecutionModel, Authenticator> topActions = new HashMap<>();

                realm.getAuthenticationExecutionsStream(policy.getId())
                        .filter(Objects::nonNull)
                        .filter(f -> !f.isDisabled())
                        .forEachOrdered(f -> {
                            if (f.isAuthenticatorFlow()) {
                                final Map<AuthenticationExecutionModel, ConditionalAuthenticator> conditions = new HashMap<>();
                                final Map<AuthenticationExecutionModel, Authenticator> actions = new HashMap<>();

                                var policyExecutions = realm.getAuthenticationExecutionsStream(f.getFlowId())
                                        .filter(g -> !g.isDisabled())
                                        .map(g -> {
                                            if (g.isAuthenticatorFlow()) {
                                                throw new IllegalStateException(String.format("No layered policies are allowed at this moment ('%s')", g.getAuthenticator()));
                                            }

                                            if (!g.isConditional()) {
                                                throw new IllegalStateException(String.format("Authentication policies can be only conditional ('%s')", g.getAuthenticator()));
                                            }

                                            var authFactory = context.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, f.getAuthenticator());
                                            if (authFactory == null) {
                                                throw new IllegalStateException(String.format("Cannot find '%s' authenticator", f.getAuthenticator()));
                                            }

                                            var authenticator = authFactory.create(session);
                                            if (authenticator instanceof ConditionalAuthenticator conditionalAuthenticator) {
                                                conditions.put(f, conditionalAuthenticator);
                                            } else {
                                                actions.put(f, authenticator);
                                            }

                                            return Map.entry(g, authenticator);
                                        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                                if (policyExecutions.values().stream().anyMatch(e -> e.requiresUser() != requiresUser)) {
                                    logger.debug("Executions should not be executed in this authentication flow process phase");
                                    return;
                                }

                                var allConditionsMatch = conditions.entrySet().stream().allMatch((entry) -> {
                                    var finalContext = new AuthenticationFlowContextWrapper(realm, context, entry.getKey().getAuthenticatorConfig()); // TODO not very good
                                    return entry.getValue().matchCondition(finalContext);
                                });

                                if (allConditionsMatch) {
                                    actions.values().forEach(e -> e.authenticate(context));
                                } else {
                                    logger.debugf("All conditions are not met for flow '%s'", f.getFlowId());
                                }
                                return;
                            }

                            var authFactory = context.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, f.getAuthenticator());
                            if (authFactory == null) {
                                throw new IllegalStateException(String.format("Cannot find '%s' authenticator", f.getAuthenticator()));
                            }

                            var authenticator = authFactory.create(session);
                            if (authenticator instanceof ConditionalAuthenticator conditionalAuthenticator) {
                                topConditions.put(f, conditionalAuthenticator);
                            } else {
                                topActions.put(f, authenticator);
                            }
                        });
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
