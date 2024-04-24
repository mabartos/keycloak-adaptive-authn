package org.keycloak.adaptive.context.location;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.UserContextCondition;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public class LocationCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final LocationContext locationContext;
    private final Set<Operation<LocationContext>> rules;

    public LocationCondition(KeycloakSession session, Set<Operation<LocationContext>> rules) {
        this.session = session;
        this.locationContext = ContextUtils.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
        this.rules = rules;
    }


    @Override
    public Set<UserContext<?>> getUserContexts() {
        return Set.of(locationContext);
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        // TODO
        return false;
    }
}
