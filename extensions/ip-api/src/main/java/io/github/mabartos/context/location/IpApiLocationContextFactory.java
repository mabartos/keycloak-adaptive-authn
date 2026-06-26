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

import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverChain;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.List;

/**
 * SPI factory for the IP API location extension ({@value #PROVIDER_ID}).
 *
 * <p>Builds an {@link IpApiLocationContext} backed by an ordered {@link GeoIpResolver} chain
 * registered via the internal GeoIP resolver SPI. Settings are read through SmallRye Config
 * ({@code Configuration}), typically via {@code KC_ADAPTIVE_*} environment variables mapped to
 * {@code kc.adaptive.*} property keys, not from SPI {@code keycloak.conf} keys.</p>
 * <ul>
 *   <li>{@link GeoIpResolverChain#PROVIDERS_PROPERTY} — comma-separated resolver ids (try order).
 *       Default: {@value GeoIpResolverIds#IPAPI_CO_FREE}, or {@value GeoIpResolverIds#IPAPI_CO_PRO}
 *       when {@link GeoIpResolverChain#IPAPI_TOKEN_PROPERTY} is set and providers were not configured.</li>
 *   <li>Known ids: {@code ipapi-co-free}, {@code ipapi-co-pro}, {@code ip-api-com-free}, {@code ip-api-com-pro}.</li>
 *   <li>{@link GeoIpResolverChain#IPAPI_TOKEN_PROPERTY} — required for {@code ipapi-co-pro}.</li>
 *   <li>{@link IpApiComGeoIpResolverProFactory#API_KEY_PROPERTY} — required for {@code ip-api-com-pro}.</li>
 *   <li>If every resolver fails, {@link IpApiLocationContext} returns {@link java.util.Optional#empty()}
 *       (not cached, ERROR logged).</li>
 * </ul>
 */
public class IpApiLocationContextFactory implements UserContextFactory<LocationContext> {

    /** Stable SPI id — do not rename (realm flows and {@link io.github.mabartos.context.UserContexts} lookups). */
    public static final String PROVIDER_ID = "ip-api-location-context";

    @Override
    public void init(Config.Scope config) {
        GeoIpResolverChain.configure();
    }

    @Override
    public IpApiLocationContext create(KeycloakSession session) {
        List<GeoIpResolver> resolvers = GeoIpResolverChain.resolve(session);
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
}
