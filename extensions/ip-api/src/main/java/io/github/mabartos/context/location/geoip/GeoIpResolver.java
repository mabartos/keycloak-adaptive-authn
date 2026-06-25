package io.github.mabartos.context.location.geoip;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.location.LocationData;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.Optional;

/**
 * One GeoIP lookup backend. Instances are obtained from the Keycloak provider registry and
 * tried in order by {@link GeoIpResolverChain} (see {@code kc.adaptive.location.providers}).
 */
public interface GeoIpResolver extends Provider {

    @Nonnull
    String id();

    Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip);

    @Override
    default void close() {
    }
}
