package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

/**
 * LocationContext backed by the JVM-wide location cache.
 */
public class GlobalCacheLocationContext extends LocationContext {
    private static final Logger log = Logger.getLogger(GlobalCacheLocationContext.class);

    private final IpAddressContext ipAddressContext;

    public GlobalCacheLocationContext(KeycloakSession session) {
        super(session);
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    @Override
    protected int maxInitDataAttempts() {
        return 1;
    }

    @Override
    public Optional<LocationData> initData(@Nonnull RealmModel realm) {
        IPAddress currentIp = ipAddressContext.getData(realm).orElse(null);
        if (currentIp == null) {
            log.trace("No IP address available");
            return Optional.empty();
        }

        Optional<LocationData> cached = GlobalLocationCache.get(currentIp);
        if (cached.isPresent()) {
            LocationData loc = cached.get();
            log.tracef(
                    "GeoIP global cache hit for ip=%s realm=%s country=%s city=%s",
                    currentIp,
                    realm.getName(),
                    loc.getCountry(),
                    loc.getCity());
            return cached;
        }
        log.tracef("GeoIP global cache miss for ip=%s realm=%s", currentIp, realm.getName());
        return Optional.empty();
    }

    public void updateCache(IPAddress ip, LocationData location) {
        if (ip == null || location == null) {
            return;
        }

        GlobalLocationCache.put(ip, location);
    }
}
