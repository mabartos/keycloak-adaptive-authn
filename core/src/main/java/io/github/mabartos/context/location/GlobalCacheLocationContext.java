package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.spi.context.CacheableUserContext;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

/**
 * LocationContext backed by the JVM-wide location cache.
 */
public class GlobalCacheLocationContext extends LocationContext implements CacheableUserContext<LocationData> {
    private static final Logger log = Logger.getLogger(GlobalCacheLocationContext.class);

    private final IpAddressContext ipAddressContext;

    public GlobalCacheLocationContext(KeycloakSession session) {
        super(session);
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    @Override
    public Optional<LocationData> initData(@Nonnull RealmModel realm) {
        IPAddress currentIp = ipAddressContext.getData(realm).orElse(null);
        if (currentIp == null) {
            log.trace("No IP address available");
            return Optional.empty();
        }

        return GlobalLocationCache.get(currentIp);
    }

    @Override
    public void updateCache(RealmModel realm, LocationData data) {
        if (data == null) {
            return;
        }

        IPAddress currentIp = ipAddressContext.getData(realm).orElse(null);
        if (currentIp == null) {
            return;
        }

        GlobalLocationCache.put(currentIp, data);
    }
}
