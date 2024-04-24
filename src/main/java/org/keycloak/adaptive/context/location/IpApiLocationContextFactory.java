package org.keycloak.adaptive.context.location;

import inet.ipaddr.IPAddress;
import org.keycloak.Config;
import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.function.Function;

public class IpApiLocationContextFactory implements UserContextFactory<LocationContext> {
    public static final String PROVIDER_ID = "ip-api-location-context";
    public static final Function<IPAddress, String> SERVICE_URL = ip -> String.format("https://ipapi.co/%s/json", ip.toFullString());

    @Override
    public LocationContext create(KeycloakSession session) {
        return new IpApiLocationContext(session);
    }

    @Override
    public void init(Config.Scope config) {

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
}
