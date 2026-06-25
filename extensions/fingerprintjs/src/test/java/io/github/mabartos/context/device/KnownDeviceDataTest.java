package io.github.mabartos.context.device;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class KnownDeviceDataTest {

    private static final String VISITOR_ID = "a3f5b2c1d4e5f678901234567890123a";
    private static final String OTHER_VISITOR_ID = "b3f5b2c1d4e5f678901234567890123b";

    @Test
    void parsesLegacyVisitorIdOnly() {
        var device = KnownDeviceData.parseFromAttribute(VISITOR_ID);

        assertThat(device, notNullValue());
        assertThat(device.visitorId(), is(VISITOR_ID));
        assertThat(device.lastSeenEpochSeconds(), nullValue());
    }

    @Test
    void parsesVisitorIdWithTimestamp() {
        var device = KnownDeviceData.parseFromAttribute(VISITOR_ID + ":1718035200");

        assertThat(device, notNullValue());
        assertThat(device.visitorId(), is(VISITOR_ID));
        assertThat(device.lastSeenEpochSeconds(), is(1_718_035_200L));
    }

    @Test
    void formatsWithTimestamp() {
        var device = KnownDeviceData.of(VISITOR_ID, 1_718_035_200L);

        assertThat(device.formatToAttribute(), is(VISITOR_ID + ":1718035200"));
    }

    @Test
    void formatsLegacyWithoutTimestamp() {
        var device = KnownDeviceData.parseFromAttribute(VISITOR_ID);

        assertThat(device.formatToAttribute(), is(VISITOR_ID));
    }

    @Test
    void parseInvalidVisitorIdReturnsNull() {
        assertThat(KnownDeviceData.parseFromAttribute("not-a-valid-id"), nullValue());
        assertThat(KnownDeviceData.parseFromAttribute(""), nullValue());
        assertThat(KnownDeviceData.parseFromAttribute(null), nullValue());
    }

    @Test
    void parsesZeroTimestampAsUndated() {
        var device = KnownDeviceData.parseFromAttribute(VISITOR_ID + ":0");

        assertThat(device, notNullValue());
        assertThat(device.lastSeenEpochSeconds(), nullValue());
        assertThat(device.isExpired(1_700_000_000L, 90), is(false));
    }

    @Test
    void detectsExpiredDevice() {
        long now = 1_700_000_000L;
        var device = KnownDeviceData.of(VISITOR_ID, now - Duration.ofDays(91).toSeconds());

        assertThat(device.isExpired(now, 90), is(true));
    }

    @Test
    void keepsFreshDevice() {
        long now = 1_700_000_000L;
        var device = KnownDeviceData.of(VISITOR_ID, now - Duration.ofDays(30).toSeconds());

        assertThat(device.isExpired(now, 90), is(false));
    }

    @Test
    void neverExpiresWhenTtlDisabled() {
        long now = 1_700_000_000L;
        var device = KnownDeviceData.of(VISITOR_ID, now - Duration.ofDays(365).toSeconds());

        assertThat(device.isExpired(now, 0), is(false));
    }

    @Test
    void ensureLastSeen_backfillsUndatedEntriesOnly() {
        long now = 1_700_000_000L;
        var legacy = KnownDeviceData.parseFromAttribute(VISITOR_ID);
        var dated = KnownDeviceData.of(OTHER_VISITOR_ID, now - 100);

        assertThat(legacy.isUndated(), is(true));
        assertThat(legacy.ensureLastSeen(now).lastSeenEpochSeconds(), is(now));
        assertThat(dated.isUndated(), is(false));
        assertThat(dated.ensureLastSeen(now), is(dated));
    }

    @Test
    void matchesVisitorId() {
        var device = KnownDeviceData.of(VISITOR_ID, 1);

        assertThat(device.matches(VISITOR_ID), is(true));
        assertThat(device.matches(OTHER_VISITOR_ID), is(false));
    }
}
