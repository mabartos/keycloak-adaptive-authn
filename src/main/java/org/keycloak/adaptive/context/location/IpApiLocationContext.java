package org.keycloak.adaptive.context.location;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.context.ip.IpAddressUtils;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public class IpApiLocationContext implements LocationContext {
    private final KeycloakSession session;
    private final HttpClientProvider httpClientProvider;
    private final DeviceContext deviceContext; // use IpAddressContext later
    private boolean isInitialized = false;
    private IpApiLocationData data;

    public IpApiLocationContext(KeycloakSession session) {
        this.session = session;
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        initData();
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void initData() {
        try {
            var client = httpClientProvider.getHttpClient();

            var uriString = Optional.ofNullable(deviceContext)
                    .map(UserContext::getData)
                    .map(DeviceRepresentation::getIpAddress)
                    .filter(StringUtil::isNotBlank)
                    .flatMap(IpAddressUtils::getIpAddress)
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

    @Override
    public LocationData getData() {
        return data;
    }
}
