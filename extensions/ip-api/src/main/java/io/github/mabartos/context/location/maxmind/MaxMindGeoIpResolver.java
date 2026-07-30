package io.github.mabartos.context.location.maxmind;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

/**
 * GeoIP via local MaxMind GeoLite2-City database ({@value GeoIpResolverIds#MAXMIND}).
 */
public final class MaxMindGeoIpResolver implements GeoIpResolver {

    private static final Logger log = Logger.getLogger(MaxMindGeoIpResolver.class);

    private final MaxMindDatabaseManager databaseManager;

    public MaxMindGeoIpResolver() {
        this(MaxMindDatabaseManager.getInstance());
    }

    MaxMindGeoIpResolver(MaxMindDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    @Nonnull
    public String id() {
        return GeoIpResolverIds.MAXMIND;
    }

    @Override
    public Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip) {
        Optional<DatabaseReader> reader = databaseManager.getReader();
        if (reader.isEmpty()) {
            log.trace("MaxMind DatabaseReader not available; skipping lookup.");
            return Optional.empty();
        }

        try {
            InetAddress address = InetAddress.getByName(ip.toString());
            return reader.get().tryCity(address).flatMap(MaxMindLocationData::from);
        } catch (IOException | GeoIp2Exception e) {
            log.warnf(e, "MaxMind lookup failed for %s", ip);
            return Optional.empty();
        }
    }
}
