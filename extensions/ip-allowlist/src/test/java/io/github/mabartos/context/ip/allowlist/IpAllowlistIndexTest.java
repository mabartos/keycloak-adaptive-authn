package io.github.mabartos.context.ip.allowlist;

import io.github.mabartos.context.ip.IPAddress;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class IpAllowlistIndexTest {

    @Test
    void lookupUsesMergedRanges() {
        var index = IpAllowlistIndex.of(List.of(
                IpAllowlistMatcher.parseEntry("10.0.0.1-10.0.0.10").orElseThrow(),
                IpAllowlistMatcher.parseEntry("10.0.0.5-10.0.0.20").orElseThrow()));

        assertThat(index.size(), is(1));
        assertThat(index.contains(IPAddress.parse("10.0.0.1")), is(true));
        assertThat(index.contains(IPAddress.parse("10.0.0.20")), is(true));
        assertThat(index.contains(IPAddress.parse("10.0.0.21")), is(false));
    }

    @Test
    void emptyListReturnsNoMatch() {
        var index = IpAllowlistIndex.empty();

        assertThat(index.isEmpty(), is(true));
        assertThat(index.contains(IPAddress.parse("127.0.0.1")), is(false));
    }

    @Test
    void overlappingCidrAndRangeAreMerged() {
        var index = IpAllowlistIndex.of(List.of(
                IpAllowlistMatcher.parseEntry("10.0.0.0/24").orElseThrow(),
                IpAllowlistMatcher.parseEntry("10.0.0.128-10.0.0.255").orElseThrow()));

        assertThat(index.contains(IPAddress.parse("10.0.0.42")), is(true));
        assertThat(index.contains(IPAddress.parse("10.0.0.200")), is(true));
    }
}
