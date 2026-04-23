package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.spi.context.UserContext;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves {@link LocationData} using an ordered list of {@link GeoIpResolver} backends
 * (e.g. {@code ipapi-co-free} then {@code ip-api-com-free}) so a later resolver runs only if earlier ones fail.
 */
public class GeoIpLocationContext extends LocationContext {
    private static final Logger log = Logger.getLogger(GeoIpLocationContext.class);

    private final IpAddressContext ipAddressContext;
    private final List<GeoIpResolver> resolvers;

    public GeoIpLocationContext(KeycloakSession session, List<GeoIpResolver> resolvers) {
        super(session);
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public Optional<LocationData> initData(@Nonnull RealmModel realm) {
        var ipAddress = Optional.ofNullable(ipAddressContext)
                .map(f -> f.getData(realm))
                .flatMap(f -> f.stream().findAny())
                .orElse(null);
        if (ipAddress == null) {
            log.tracef("Cannot obtain IP address");
            return Optional.empty();
        }

        String realmName = realm.getName();
        log.tracef(
                "GeoIP resolution start realm=%s ip=%s resolverChain=[%s]",
                realmName,
                ipAddress,
                resolvers.stream().map(GeoIpResolver::id).collect(Collectors.joining(", ")));

        int total = resolvers.size();
        for (int i = 0; i < total; i++) {
            GeoIpResolver resolver = resolvers.get(i);
            log.tracef(
                    "GeoIP trying resolver id=%s for ip=%s realm=%s (%d/%d)",
                    resolver.id(), ipAddress, realmName, i + 1, total);

            Optional<LocationData> data = resolver.resolve(session, realm, ipAddress);
            if (data.isPresent()) {
                LocationData resolved = data.get();
                log.tracef(
                        "GeoIP location obtained from resolver id=%s for ip=%s realm=%s (country=%s, city=%s)",
                        resolver.id(),
                        ipAddress,
                        realmName,
                        resolved.getCountry(),
                        resolved.getCity());
                UserContext<?> globalCacheCtx =
                        UserContexts.getContext(session, GlobalCacheLocationContextFactory.PROVIDER_ID);
                if (globalCacheCtx instanceof GlobalCacheLocationContext globalCache) {
                    globalCache.updateCache(ipAddress, resolved);
                }
                UserContext<?> authnCtx =
                        UserContexts.getContext(session, AuthnSessionLocationContextFactory.PROVIDER_ID);
                if (authnCtx instanceof AuthnSessionLocationContext authnSession) {
                    authnSession.updateCache(ipAddress, resolved);
                }
                return data;
            }

            if (i + 1 < total) {
                String nextId = resolvers.get(i + 1).id();
                log.tracef(
                        "GeoIP resolver id=%s returned no location for ip=%s realm=%s; falling back to resolver id=%s",
                        resolver.id(),
                        ipAddress,
                        realmName,
                        nextId);
            } else {
                log.tracef(
                        "GeoIP resolver id=%s returned no location for ip=%s realm=%s; no further resolvers in chain",
                        resolver.id(),
                        ipAddress,
                        realmName);
            }
        }

        log.errorf(
                "GeoIP all %d resolver(s) failed for ip=%s realm=%s; using placeholder location (all string fields=%s, not cached)",
                total,
                ipAddress,
                realmName,
                UnknownLocationData.UNKNOWN);
        return Optional.of(UnknownLocationData.INSTANCE);
    }
}

/**
 * One HTTP GeoIP backend; implementations are tried in order by {@link GeoIpLocationContext}.
 */
interface GeoIpResolver {

    @Nonnull
    String id();

    Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip);
}
