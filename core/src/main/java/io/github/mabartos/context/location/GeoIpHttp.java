package io.github.mabartos.context.location;

import io.github.mabartos.context.ip.IPAddress;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.regex.Pattern;

final class GeoIpHttp {
    private static final Logger log = Logger.getLogger(GeoIpHttp.class);

    /** Redacts {@code key=} and {@code token=} query values for safe logging (encoded or not). */
    private static final Pattern SENSITIVE_QUERY_PARAM = Pattern.compile("([?&])(key|token)=[^&]*");

    private GeoIpHttp() {
    }

    private static String redactUriForLog(URI uri) {
        return SENSITIVE_QUERY_PARAM.matcher(uri.toString()).replaceAll("$1$2=*****");
    }

    /**
     * Encode an IP for use inside a URI path segment (IPv6 colons are escaped).
     */
    static String encodeIpForPath(IPAddress ip) {
        String s = ip.toString();
        if (ip.isIPv6() && !ip.isIPv4Convertible()) {
            return s.replace(":", "%3A");
        }
        return s;
    }

    static Optional<InputStream> getJsonStream(KeycloakSession session, URI uri) {
        var client = session.getProvider(HttpClientProvider.class).getHttpClient();
        var getRequest = new HttpGet(uri);
        try (var response = client.execute(getRequest)) {
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                EntityUtils.consumeQuietly(response.getEntity());
                String safeUri = redactUriForLog(uri);
                if (code == 403) {
                    log.warnf("GeoIP HTTP %s for %s", code, safeUri);
                } else {
                    log.tracef("GeoIP HTTP %s for %s", code, safeUri);
                }
                return Optional.empty();
            }
            if (response.getEntity() == null) {
                return Optional.empty();
            }
            byte[] body = response.getEntity().getContent().readAllBytes();
            return Optional.of(new java.io.ByteArrayInputStream(body));
        } catch (IOException | RuntimeException e) {
            log.warnf(e, "GeoIP request failed for %s", redactUriForLog(uri));
            return Optional.empty();
        }
    }
}
