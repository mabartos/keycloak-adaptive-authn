package org.keycloak.adaptive.context.agent;

import org.keycloak.adaptive.spi.policy.UserContextRule;
import org.keycloak.adaptive.spi.policy.UserContextRules;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;
import java.util.Map;

public class UserAgentRules implements UserContextRules<UserAgentContext, String> {
    private final KeycloakSession session;
    private final Map<String, UserContextRule<UserAgentContext>> rules;

    public UserAgentRules(KeycloakSession session, Map<String, UserContextRule<UserAgentContext>> rules) {
        this.session = session;
        this.rules = rules;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean matchesConditions(Map<String, String> values) {
        var ua = session.getProvider(UserAgentContext.class);
        return values.entrySet()
                .stream()
                .filter(f -> rules.containsKey(f.getKey()))
                .allMatch(s -> rules.get(s.getKey()).match(ua, s.getValue()));
    }

    @Override
    public Collection<UserContextRule<UserAgentContext>> getRules() {
        return rules.values();
    }
}
