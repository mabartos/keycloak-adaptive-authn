package io.github.mabartos.context.ip;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class IpAddressUtilsTest {

    @Test
    public void testValidIpv4Address() {
        IPAddress ip = new IPAddressString("192.168.1.1").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isIPv4(), is(true));
    }

    @Test
    public void testValidIpv6Address() {
        IPAddress ip = new IPAddressString("2001:0db8:85a3:0000:0000:8a2e:0370:7334").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isIPv6(), is(true));
    }

    @Test
    public void testLocalhostIpv4() {
        IPAddress ip = new IPAddressString("127.0.0.1").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isLoopback(), is(true));
    }

    @Test
    public void testLocalhostIpv6() {
        IPAddress ip = new IPAddressString("::1").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isLoopback(), is(true));
    }

    @Test
    public void testPrivateIpv4Addresses() {
        IPAddress ip1 = new IPAddressString("10.0.0.1").getAddress();
        assertThat(ip1, notNullValue());
        assertThat(ip1.isIPv4(), is(true));

        IPAddress ip2 = new IPAddressString("172.16.0.1").getAddress();
        assertThat(ip2, notNullValue());
        assertThat(ip2.isIPv4(), is(true));

        IPAddress ip3 = new IPAddressString("192.168.0.1").getAddress();
        assertThat(ip3, notNullValue());
        assertThat(ip3.isIPv4(), is(true));
    }

    @Test
    public void testPublicIpv4Address() {
        IPAddress ip = new IPAddressString("8.8.8.8").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isIPv4(), is(true));
        assertThat(ip.isLoopback(), is(false));
    }

    @Test
    public void testInvalidIpAddress() {
        IPAddress ip = new IPAddressString("999.999.999.999").getAddress();
        assertThat(ip, is((IPAddress) null));
    }

    @Test
    public void testIpAddressComparison() {
        IPAddress ip1 = new IPAddressString("192.168.1.1").getAddress();
        IPAddress ip2 = new IPAddressString("192.168.1.1").getAddress();
        IPAddress ip3 = new IPAddressString("192.168.1.2").getAddress();

        assertThat(ip1.equals(ip2), is(true));
        assertThat(ip1.equals(ip3), is(false));
    }

    @Test
    public void testIpAddressInRange() {
        IPAddress ip = new IPAddressString("192.168.1.50").getAddress();
        IPAddress rangeStart = new IPAddressString("192.168.1.1").getAddress();
        IPAddress rangeEnd = new IPAddressString("192.168.1.100").getAddress();

        assertThat(ip.compareTo(rangeStart) >= 0, is(true));
        assertThat(ip.compareTo(rangeEnd) <= 0, is(true));
    }

    @Test
    public void testIpv4MappedIpv6() {
        IPAddress ip = new IPAddressString("::ffff:192.168.1.1").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isIPv6(), is(true));
        assertThat(ip.isIPv4Convertible(), is(true));
    }

    @Test
    public void testLinkLocalIpv6() {
        IPAddress ip = new IPAddressString("fe80::1").getAddress();
        assertThat(ip, notNullValue());
        assertThat(ip.isLinkLocal(), is(true));
    }
}
