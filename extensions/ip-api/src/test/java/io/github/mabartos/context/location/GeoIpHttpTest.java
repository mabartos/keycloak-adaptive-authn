package io.github.mabartos.context.location;

import io.github.mabartos.context.ip.IPAddress;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Documents path encoding for GeoIP providers (ipapi.co, ip-api.com).
 */
class GeoIpHttpTest {

    @Test
    void encodeIpForPath_leavesIpv4Unchanged() {
        IPAddress ip = IPAddress.parse("203.0.113.1");

        assertThat(GeoIpHttp.encodeIpForPath(ip), is("203.0.113.1"));
    }

    @Test
    void encodeIpForPath_percentEncodesIpv6ColonsInPathSegment() {
        IPAddress ip = IPAddress.parse("2001:db8::1");

        String encoded = GeoIpHttp.encodeIpForPath(ip);
        assertThat(encoded.contains(":"), is(false));
        assertThat(encoded, is("2001%3Adb8%3A0%3A0%3A0%3A0%3A0%3A1"));
    }

    @Test
    void encodeIpForPath_ipv4MappedUsesCanonicalIpv4Literal() {
        IPAddress ip = IPAddress.parse("::ffff:8.8.8.8");

        assertThat(GeoIpHttp.encodeIpForPath(ip), is("8.8.8.8"));
    }
}
