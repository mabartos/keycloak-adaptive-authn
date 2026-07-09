package io.github.mabartos.context.ip.whitelist;

import io.github.mabartos.context.ip.IPAddress;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class IpWhitelistMatcherTest {

    @Test
    void singleIpv4Match() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of("203.0.113.42"));

        assertThat(matcher.contains(IPAddress.parse("203.0.113.42")), is(true));
        assertThat(matcher.contains(IPAddress.parse("203.0.113.43")), is(false));
    }

    @Test
    void hyphenRangeMatch() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of("10.0.0.1-10.0.0.255"));

        assertThat(matcher.contains(IPAddress.parse("10.0.0.1")), is(true));
        assertThat(matcher.contains(IPAddress.parse("10.0.0.128")), is(true));
        assertThat(matcher.contains(IPAddress.parse("10.0.1.1")), is(false));
    }

    @Test
    void cidrMatch() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of("10.0.0.0/8", "192.168.1.0/24"));

        assertThat(matcher.contains(IPAddress.parse("10.42.0.1")), is(true));
        assertThat(matcher.contains(IPAddress.parse("192.168.1.42")), is(true));
        assertThat(matcher.contains(IPAddress.parse("192.168.2.1")), is(false));
    }

    @Test
    void allInvalidEntriesProduceEmptyMatcher() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of(
                "not-an-ip",
                "2001:db8::1",
                "10.0.0.1-10.0.0",
                "10.0.0.0/99"));

        assertThat(matcher.isEmpty(), is(true));
    }

    @Test
    void invalidEntriesAreIgnored() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of(
                "not-an-ip",
                "2001:db8::1",
                "10.0.0.1-10.0.0",
                "10.0.0.0/99",
                "203.0.113.1"));

        assertThat(matcher.contains(IPAddress.parse("203.0.113.1")), is(true));
        assertThat(matcher.isEmpty(), is(false));
    }

    @Test
    void ipv6ClientIsRejected() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of("203.0.113.1"));

        assertThat(matcher.contains(IPAddress.parse("2001:db8::1")), is(false));
    }

    @Test
    void ipv4MappedClientIsTreatedAsIpv4() {
        var matcher = IpWhitelistMatcher.fromEntries(List.of("192.168.1.10"));

        assertThat(matcher.contains(IPAddress.parse("::ffff:192.168.1.10")), is(true));
    }

    @Test
    void isValidEntryRejectsIpv6() {
        assertThat(IpWhitelistMatcher.isValidEntry("2001:db8::1"), is(false));
        assertThat(IpWhitelistMatcher.isValidEntry("10.0.0.0/8"), is(true));
    }
}
