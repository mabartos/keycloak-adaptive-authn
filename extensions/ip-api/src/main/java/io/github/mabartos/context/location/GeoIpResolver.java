package io.github.mabartos.context.location;

import io.github.mabartos.context.ip.IPAddress;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

/**
 * One HTTP GeoIP backend; implementations are tried in order by {@link IpApiLocationContext}.
 */
interface GeoIpResolver {

    @Nonnull
    String id();

    Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip);
}
