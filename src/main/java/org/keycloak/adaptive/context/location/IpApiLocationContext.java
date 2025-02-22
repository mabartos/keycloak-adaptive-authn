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
package org.keycloak.adaptive.context.location;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.ip.client.DefaultIpAddressFactory;
import org.keycloak.adaptive.context.ip.client.IpAddressContext;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Obtain location data based on the IP address from 'ipapi.co' server
 */
public class IpApiLocationContext extends LocationContext {
    private static final Logger log = Logger.getLogger(IpApiLocationContext.class);
    private final KeycloakSession session;
    private final HttpClientProvider httpClientProvider;

    public IpApiLocationContext(KeycloakSession session) {
        this.session = session;
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Optional<LocationData> initData() {
        try {
            final IpAddressContext ipAddressContext = UserContexts.getContext(session, DefaultIpAddressFactory.PROVIDER_ID);

            var client = httpClientProvider.getHttpClient();

            var uriString = Optional.ofNullable(ipAddressContext)
                    .map(UserContext::getData)
                    .flatMap(f->f.stream().findAny())
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
                return Optional.ofNullable(JsonSerialization.readValue(response.getEntity().getContent(), IpApiLocationData.class));
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            log.error(e);
        }
        return Optional.empty();
    }
}
