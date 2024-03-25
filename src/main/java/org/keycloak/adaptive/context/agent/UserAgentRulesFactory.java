package org.keycloak.adaptive.context.agent;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.UserContextRule;
import org.keycloak.adaptive.spi.policy.UserContextRules;
import org.keycloak.adaptive.spi.policy.UserContextRulesFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

public class UserAgentRulesFactory implements UserContextRulesFactory<UserAgentContext> {
    public static final String PROVIDER_ID = "default-user-agent-rules-factory";
    private Map<String, UserContextRule<UserAgentContext>> rules;

    @Override
    public UserContextRules<UserAgentContext, String> create(KeycloakSession session) {
        return new UserAgentRules(session, rules);
    }

    @Override
    public void init(Config.Scope config) {
        this.rules = Map.ofEntries(
                new UserContextRule<>("EQ", "is equal to",
                        (ua, val) -> ua.getData().getName().equals(val)),
                new UserContextRule<>("NEQ", "is equal to",
                        (ua, val) -> ua.getData().getName().equals(val)),
                new UserContextRule<>("AO", "is any of",
                        (ua, val) -> List.of(val.split(",")).contains(ua.getData().getName())),
                new UserContextRule<>("NO", "is none of",
                        (ua, val) -> !List.of(val.split(",")).contains(ua.getData().getName()))
        );
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public Map<String, UserContextRule<UserAgentContext>> getRules() {
        return rules;
    }
}
