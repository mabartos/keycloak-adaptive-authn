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

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.client.IpAddressContext;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * LocationContext that caches location data in the authentication session.
 * Avoids calling external location services when the IP address hasn't changed.
 */
public class AuthnSessionLocationContext extends LocationContext {
    private static final Logger log = Logger.getLogger(AuthnSessionLocationContext.class);

    private static final String LAST_IP_KEY = "ADAPTIVE_AUTHN_LOCATION_LAST_IP";
    private static final String LAST_LOCATION_KEY = "ADAPTIVE_AUTHN_LOCATION_LAST_DATA";

    private final IpAddressContext ipAddressContext;

    public AuthnSessionLocationContext(KeycloakSession session) {
        super(session);
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    @Override
    public Optional<LocationData> initData(@Nonnull RealmModel realm) {
        var authSession = session.getContext().getAuthenticationSession();
        if (authSession == null) {
            log.trace("No authentication session available");
            return Optional.empty();
        }

        IPAddress currentIp = ipAddressContext.getData(realm).orElse(null);
        if (currentIp == null) {
            log.trace("No IP address available");
            return Optional.empty();
        }

        // Check cached IP and location
        var cachedIp = authSession.getAuthNote(LAST_IP_KEY);
        var cachedLocation = authSession.getAuthNote(LAST_LOCATION_KEY);

        // If IP hasn't changed and we have cached location, reuse it
        if (StringUtil.isNotBlank(cachedIp) && cachedIp.equals(currentIp.toString()) && StringUtil.isNotBlank(cachedLocation)) {
            log.tracef("IP address unchanged (%s), using cached location: %s", cachedIp, cachedLocation);
            return parseCachedLocation(cachedLocation);
        }

        return Optional.empty();
    }

    public void updateCache(IPAddress ip, LocationData location) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
        if (authSession == null || ip == null || location == null) {
            return;
        }

        authSession.setAuthNote(LAST_IP_KEY, ip.toString());
        String locationString = LocationDataUtils.formatToAttribute(location);
        authSession.setAuthNote(LAST_LOCATION_KEY, locationString);
        log.tracef("Updated location cache: IP=%s, Location=%s", ip, locationString);
    }

    private Optional<LocationData> parseCachedLocation(String cached) {
        return Optional.ofNullable(LocationDataUtils.parseFromAttribute(cached));
    }
}
