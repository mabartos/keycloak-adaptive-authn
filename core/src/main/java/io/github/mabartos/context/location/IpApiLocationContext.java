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

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.client.IpAddressContext;
import jakarta.annotation.Nonnull;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Obtain location data based on the IP address from 'ipapi.co' server
 */
public class IpApiLocationContext extends LocationContext {

    private static final Logger log = Logger.getLogger(IpApiLocationContext.class);

    private static final String LAST_IP_KEY = "ADAPTIVE_AUTHN_LOCATION_LAST_IP";
    private static final String LAST_LOCATION_KEY = "ADAPTIVE_AUTHN_LOCATION_LAST_DATA";

    private final HttpClientProvider httpClientProvider;
    private final IpAddressContext ipAddressContext;

    /* Override IP with random placeholder for debug */
    private static final List<String> DEBUG_IPS = List.of(
            "176.150.253.172", // France
            "191.101.157.70",  // Germany
            "79.142.79.54",    // Switzerland
            "212.112.19.28",   // Sweden
            "212.102.49.216"   // Spain
    );

    public IpApiLocationContext(KeycloakSession session) {
        super(session);
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    private IPAddress resolveIpForDebug(IPAddress realIp) {
        String forcedIp = System.getenv("KC_SPI_ADAPTATIVE_AUTHN_DEBUG_RANDOM_IP_ADRESSES");
        if (forcedIp != null && !forcedIp.isBlank()) {
            IPAddress parsed = new IPAddressString(forcedIp).getAddress();
            if (parsed != null) {
                log.tracef("Forced debug IP from env: %s instead of %s", parsed, realIp);
                return parsed;
            }
            log.warnf("Invalid DEBUG_IP value: %s", forcedIp);
        }

        String randomEnabled = System.getenv("KC_SPI_ADAPTATIVE_AUTHN_DEBUG_RANDOM_IP");
        if ("true".equalsIgnoreCase(randomEnabled) && !DEBUG_IPS.isEmpty()) {
            String picked = DEBUG_IPS.get(ThreadLocalRandom.current().nextInt(DEBUG_IPS.size()));
            IPAddress parsed = new IPAddressString(picked).getAddress();
            if (parsed != null) {
                log.tracef("Random debug IP from env: %s instead of %s", parsed, realIp);
                return parsed;
            }
            log.warnf("Invalid IP in DEBUG_IPS: %s", picked);
        }

        return realIp;
    }

    @Override
    public Optional<LocationData> initData(@Nonnull RealmModel realm) {
        try {
            var ipAddress = Optional.ofNullable(ipAddressContext)
                    .map(f -> f.getData(realm))
                    .flatMap(f -> f.stream().findAny())
                    .orElse(null);

            if (ipAddress == null) {
                log.trace("Cannot obtain IP address");
                return Optional.empty();
            }

            var effectiveIpAddress = resolveIpForDebug(ipAddress);
            var authSession = session.getContext().getAuthenticationSession();

            // 1. Authentication session cache
            Optional<LocationData> sessionCached = getFromAuthSessionCache(authSession, effectiveIpAddress);
            if (sessionCached.isPresent()) {
                log.tracef("Using authentication session cached location for IP %s", effectiveIpAddress);
                return sessionCached;
            }

            // 2. Global JVM cache
            Optional<LocationData> globalCached = GlobalLocationCache.get(effectiveIpAddress);
            if (globalCached.isPresent()) {
                log.tracef("Using global cached location for IP %s", effectiveIpAddress);
                updateAuthSessionCache(authSession, effectiveIpAddress, globalCached.get());
                return globalCached;
            }

            // 3. Remote API call
            var client = httpClientProvider.getHttpClient();
            var uriString = Optional.of(effectiveIpAddress)
                    .map(IpApiLocationContextFactory.SERVICE_URL)
                    .filter(StringUtil::isNotBlank);

            if (uriString.isEmpty()) {
                log.error("Cannot obtain full URL for IP API");
                return Optional.empty();
            }

            var getRequest = new HttpGet(new URIBuilder(uriString.get()).build());

            try (var response = client.execute(getRequest)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    log.error(response.getStatusLine().getReasonPhrase());
                    return Optional.empty();
                }

                Optional<LocationData> data = Optional.ofNullable(
                        JsonSerialization.readValue(
                                response.getEntity().getContent(),
                                IpApiLocationData.class));

                data.ifPresent(location -> {
                    log.tracef("Location obtained: %s", location);
                    updateAuthSessionCache(authSession, effectiveIpAddress, location);
                    GlobalLocationCache.put(effectiveIpAddress, location);
                });

                return data;
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            log.error("Failed to initialize location data", e);
        }

        return Optional.empty();
    }

    private Optional<LocationData> getFromAuthSessionCache(AuthenticationSessionModel authSession, IPAddress currentIp) {
        if (authSession == null) {
            log.trace("No authentication session available");
            return Optional.empty();
        }

        var cachedIp = authSession.getAuthNote(LAST_IP_KEY);
        var cachedLocation = authSession.getAuthNote(LAST_LOCATION_KEY);
        var currentIpString = currentIp.toString();

        if (StringUtil.isNotBlank(cachedIp)
                && cachedIp.equals(currentIpString)
                && StringUtil.isNotBlank(cachedLocation)) {
            log.tracef("IP address unchanged (%s), using authentication session cached location: %s",
                    cachedIp, cachedLocation);
            return Optional.ofNullable(LocationDataUtils.parseFromAttribute(cachedLocation));
        }

        return Optional.empty();
    }

    private void updateAuthSessionCache(AuthenticationSessionModel authSession, IPAddress ip, LocationData location) {
        if (authSession == null || ip == null || location == null) {
            return;
        }

        String locationString = LocationDataUtils.formatToAttribute(location);
        authSession.setAuthNote(LAST_IP_KEY, ip.toString());
        authSession.setAuthNote(LAST_LOCATION_KEY, locationString);

        log.tracef("Updated authentication session location cache: IP=%s, Location=%s", ip, locationString);
    }
}