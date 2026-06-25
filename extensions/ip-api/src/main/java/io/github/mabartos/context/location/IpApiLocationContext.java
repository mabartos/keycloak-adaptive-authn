/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.location.geoip.GeoIpResolver;
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
 *
 * <p>If every resolver fails, returns {@link Optional#empty()}. Nothing is cached here; upper
 * cache layers ({@link GlobalCacheLocationContext}, {@link AuthnSessionLocationContext}) are updated
 * by {@link io.github.mabartos.spi.context.AbstractUserContext} when this context is used as a delegate.
 * {@link LocationConditionFactory} maps absent data to {@code "<unknown>"} for country/city conditions.</p>
 */
public class IpApiLocationContext extends LocationContext {
    private static final Logger log = Logger.getLogger(IpApiLocationContext.class);

    private final IpAddressContext ipAddressContext;
    private final List<GeoIpResolver> resolvers;

    public IpApiLocationContext(KeycloakSession session, List<GeoIpResolver> resolvers) {
        this(session, UserContexts.getContext(session, IpAddressContext.class), resolvers);
    }

    IpApiLocationContext(KeycloakSession session, IpAddressContext ipAddressContext, List<GeoIpResolver> resolvers) {
        super(session);
        this.ipAddressContext = ipAddressContext;
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public Optional<LocationData> initData(@Nonnull RealmModel realm) {
        IPAddress ipAddress = resolveIpAddress(ipAddressContext, realm);
        return resolveForIp(session, realm, ipAddress);
    }

    static IPAddress resolveIpAddress(IpAddressContext ipAddressContext, RealmModel realm) {
        return Optional.ofNullable(ipAddressContext)
                .flatMap(ctx -> ctx.getData(realm))
                .orElse(null);
    }

    Optional<LocationData> resolveForIp(KeycloakSession session, RealmModel realm, IPAddress ipAddress) {
        if (ipAddress == null) {
            log.tracef("Cannot obtain IP address");
            return Optional.empty();
        }

        return resolveThroughResolvers(session, realm, ipAddress, resolvers);
    }

    /**
     * Tries each resolver in order. Upper cache layers ({@link GlobalCacheLocationContext},
     * {@link AuthnSessionLocationContext}) are updated by {@link io.github.mabartos.spi.context.AbstractUserContext}
     * when this context is used as a delegate.
     */
    static Optional<LocationData> resolveThroughResolvers(
            KeycloakSession session, RealmModel realm, IPAddress ipAddress, List<GeoIpResolver> resolvers) {
        String realmName = realm != null ? realm.getName() : "";
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
                "GeoIP all %d resolver(s) failed for ip=%s realm=%s; returning no location (not cached)",
                total,
                ipAddress,
                realmName);
        return Optional.empty();
    }
}
