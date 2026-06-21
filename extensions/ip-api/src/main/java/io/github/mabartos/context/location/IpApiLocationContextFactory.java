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

import io.github.mabartos.spi.context.UserContextFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SPI factory for the IP API location extension ({@value #PROVIDER_ID}).
 *
 * <p>Builds an {@link IpApiLocationContext} backed by an ordered {@link GeoIpResolver} chain
 * (ipapi.co, ip-api.com, …). Settings are read through SmallRye Config ({@code Configuration}),
 * typically via {@code KC_ADAPTIVE_*} environment variables mapped to {@code kc.adaptive.*} property keys,
 * not from SPI {@code keycloak.conf} keys.</p>
 * <ul>
 *   <li>{@value #PROVIDERS_PROPERTY} — comma-separated resolver ids (try order). Default: {@value IpApiCoGeoIpResolver#RESOLVER_ID_FREE},
 *       or {@value IpApiCoGeoIpResolver#RESOLVER_ID_PRO} when {@value #IPAPI_TOKEN_PROPERTY} is set and
 *       {@value #PROVIDERS_PROPERTY} was not configured.</li>
 *   <li>Known ids: {@code ipapi-co-free}, {@code ipapi-co-pro}, {@code ip-api-com-free}, {@code ip-api-com-pro}.</li>
 *   <li>{@value #IPAPI_TOKEN_PROPERTY} — required for {@code ipapi-co-pro}; ignored for {@code ipapi-co-free}.</li>
 *   <li>{@value #IP_API_COM_API_KEY_PROPERTY} — required for {@code ip-api-com-pro}; ignored for {@code ip-api-com-free}.</li>
 *   <li>If every resolver fails, {@link IpApiLocationContext} returns {@link java.util.Optional#empty()} (not cached, ERROR logged).</li>
 * </ul>
 */
public class IpApiLocationContextFactory implements UserContextFactory<LocationContext> {

    /** Stable SPI id — do not rename (realm flows and {@link io.github.mabartos.context.UserContexts} lookups). */
    public static final String PROVIDER_ID = "ip-api-location-context";

    public static final String PROVIDERS_PROPERTY = "kc.adaptive.location.providers";
    public static final String IPAPI_TOKEN_PROPERTY = "kc.adaptive.ipapi.token";
    public static final String IP_API_COM_API_KEY_PROPERTY = "kc.adaptive.ip-api-com.api-key";

    private static final Logger log = Logger.getLogger(IpApiLocationContextFactory.class);

    private List<GeoIpResolver> resolvers = List.of(new IpApiCoGeoIpResolver(IpApiCoGeoIpResolver.RESOLVER_ID_FREE, null));

    @Override
    public void init(Config.Scope config) {
        this.resolvers = List.copyOf(buildResolvers());
    }

    @Override
    public IpApiLocationContext create(KeycloakSession session) {
        return new IpApiLocationContext(session, resolvers);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<LocationContext> getUserContextClass() {
        return LocationContext.class;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    private static String readIpApiToken() {
        return Configuration.getOptionalValue(IPAPI_TOKEN_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    private static String readIpApiComKey() {
        return Configuration.getOptionalValue(IP_API_COM_API_KEY_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    private static List<GeoIpResolver> buildResolvers() {
        String configured = Configuration.getOptionalValue(PROVIDERS_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        String ipApiToken = readIpApiToken();
        String raw = resolveProvidersForMigration(configured, isProvidersExplicitlyConfigured(configured), ipApiToken);
        return buildResolvers(raw, ipApiToken, readIpApiComKey());
    }

    /**
     * Preserves legacy behaviour: deployments that set only {@value #IPAPI_TOKEN_PROPERTY} (no
     * {@value #PROVIDERS_PROPERTY}) default to {@value IpApiCoGeoIpResolver#RESOLVER_ID_PRO}.
     */
    static String resolveProvidersForMigration(
            String configuredProviders, boolean providersExplicitlySet, String ipApiToken) {
        String raw = configuredProviders != null && !configuredProviders.isBlank()
                ? configuredProviders.trim()
                : IpApiCoGeoIpResolver.RESOLVER_ID_FREE;
        if (!providersExplicitlySet && ipApiToken != null && !ipApiToken.isBlank()) {
            return IpApiCoGeoIpResolver.RESOLVER_ID_PRO;
        }
        return raw;
    }

    /**
     * True when {@value #PROVIDERS_PROPERTY} is set in SmallRye Config (env, properties file, etc.).
     */
    static boolean isProvidersExplicitlyConfigured(String configuredProviders) {
        return configuredProviders != null && !configuredProviders.isBlank();
    }

    /**
     * Builds the resolver chain from explicit config (used by {@link #buildResolvers()} and unit tests).
     */
    static List<GeoIpResolver> buildResolvers(String providersRaw, String ipApiToken, String ipApiComKey) {
        String raw = providersRaw != null && !providersRaw.isBlank()
                ? providersRaw.trim()
                : IpApiCoGeoIpResolver.RESOLVER_ID_FREE;
        List<GeoIpResolver> list = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            switch (id) {
                case IpApiCoGeoIpResolver.RESOLVER_ID_FREE:
                    appendResolverIfFirst(
                            list,
                            seenIds,
                            id,
                            new IpApiCoGeoIpResolver(IpApiCoGeoIpResolver.RESOLVER_ID_FREE, null),
                            raw);
                    break;
                case IpApiCoGeoIpResolver.RESOLVER_ID_PRO: {
                    if (ipApiToken == null || ipApiToken.isBlank()) {
                        log.warnf(
                                "GeoIP providers include '%s' but '%s' is blank; skipping that resolver.",
                                IpApiCoGeoIpResolver.RESOLVER_ID_PRO,
                                IPAPI_TOKEN_PROPERTY);
                        break;
                    }
                    appendResolverIfFirst(
                            list,
                            seenIds,
                            id,
                            new IpApiCoGeoIpResolver(IpApiCoGeoIpResolver.RESOLVER_ID_PRO, ipApiToken),
                            raw);
                    break;
                }
                case IpApiComGeoIpResolver.RESOLVER_ID_FREE:
                    appendResolverIfFirst(
                            list,
                            seenIds,
                            id,
                            new IpApiComGeoIpResolver(IpApiComGeoIpResolver.RESOLVER_ID_FREE, null),
                            raw);
                    break;
                case IpApiComGeoIpResolver.RESOLVER_ID_PRO: {
                    if (ipApiComKey == null || ipApiComKey.isBlank()) {
                        log.warnf(
                                "GeoIP providers include '%s' but '%s' is blank; skipping that resolver.",
                                IpApiComGeoIpResolver.RESOLVER_ID_PRO,
                                IP_API_COM_API_KEY_PROPERTY);
                        break;
                    }
                    appendResolverIfFirst(
                            list,
                            seenIds,
                            id,
                            new IpApiComGeoIpResolver(IpApiComGeoIpResolver.RESOLVER_ID_PRO, ipApiComKey),
                            raw);
                    break;
                }
                default:
                    log.warnf("Unknown GeoIP resolver id '%s' in %s=%s", id, PROVIDERS_PROPERTY, raw);
            }
        }
        if (list.isEmpty()) {
            log.warnf("No usable GeoIP resolvers after parsing %s=%s; falling back to %s.",
                    PROVIDERS_PROPERTY, raw, IpApiCoGeoIpResolver.RESOLVER_ID_FREE);
            list.add(new IpApiCoGeoIpResolver(IpApiCoGeoIpResolver.RESOLVER_ID_FREE, null));
        }
        return list;
    }

    private static void appendResolverIfFirst(
            List<GeoIpResolver> list,
            Set<String> seenIds,
            String resolverId,
            GeoIpResolver resolver,
            String providersRaw) {
        if (seenIds.add(resolverId)) {
            list.add(resolver);
        } else {
            log.warnf(
                    "Duplicate GeoIP resolver id '%s' in %s=%s; skipping.",
                    resolverId,
                    PROVIDERS_PROPERTY,
                    providersRaw);
        }
    }
}
