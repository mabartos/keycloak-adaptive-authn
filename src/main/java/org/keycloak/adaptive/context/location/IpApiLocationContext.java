package org.keycloak.adaptive.context.location;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.keycloak.adaptive.context.ContextUtils;
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

public class IpApiLocationContext extends LocationContext {
    private final KeycloakSession session;
    private final HttpClientProvider httpClientProvider;
    private final IpAddressContext ipAddressContext;
    public IpApiLocationContext(KeycloakSession session) {
        this.session = session;
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
        this.ipAddressContext = ContextUtils.getContext(session, DefaultIpAddressFactory.PROVIDER_ID);
    }

    @Override
    public void initData() {
        try {
            var client = httpClientProvider.getHttpClient();

            var uriString = Optional.ofNullable(ipAddressContext)
                    .map(UserContext::getData)
                    .flatMap(f->f.stream().findAny())
                    .map(IpApiLocationContextFactory.SERVICE_URL)
                    .filter(StringUtil::isNotBlank)
                    .orElseThrow(() -> new IllegalStateException("Cannot obtain full URL for IP API"));

            var getRequest = new HttpGet(new URIBuilder(uriString).build());

            try (var response = client.execute(getRequest)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new RuntimeException(response.getStatusLine().getReasonPhrase());
                }
                this.data = JsonSerialization.readValue(response.getEntity().getContent(), IpApiLocationData.class);
            }

            this.isInitialized = true;
        } catch (URISyntaxException | IOException | RuntimeException e) {
            this.isInitialized = false;
            throw new RuntimeException(e);
        }
    }
}
