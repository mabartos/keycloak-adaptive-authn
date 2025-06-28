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
import org.keycloak.adaptive.context.ip.client.TestIpAddressContextFactory;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.keycloak.adaptive.context.ip.client.TestIpAddressContextFactory.USE_TESTING_IP_PROP;

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

    private boolean useTestingIpAddress() {
        return Configuration.isTrue(USE_TESTING_IP_PROP);
    }

    @Override
    public Optional<LocationData> initData() {
        try {
            final var contextProvider = useTestingIpAddress() ? TestIpAddressContextFactory.PROVIDER_ID : DefaultIpAddressFactory.PROVIDER_ID;
            final IpAddressContext ipAddressContext = UserContexts.getContext(session, contextProvider);

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
                Optional<LocationData> data = Optional.ofNullable(JsonSerialization.readValue(response.getEntity().getContent(), IpApiLocationData.class));
                data.ifPresent(location -> log.tracef("Location obtained: %s", data));
                return data;
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            log.error(e);
        }
        return Optional.empty();
    }
}
