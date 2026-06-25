package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class KnownLocationDataTest {

    @Test
    void parsesLegacyCountryCityFormat() {
        var location = KnownLocationData.parseFromAttribute("France:Paris");

        assertThat(location, notNullValue());
        assertThat(location.getCountry(), is("France"));
        assertThat(location.getCity(), is("Paris"));
        assertThat(location.lastSeenEpochSeconds(), nullValue());
    }

    @Test
    void parsesCountryCityTimestampFormat() {
        var location = KnownLocationData.parseFromAttribute("France:Paris:1700000000");

        assertThat(location, notNullValue());
        assertThat(location.getCountry(), is("France"));
        assertThat(location.getCity(), is("Paris"));
        assertThat(location.lastSeenEpochSeconds(), is(1_700_000_000));
    }

    @Test
    void formatsWithTimestamp() {
        var location = KnownLocationData.of("France", "Paris", 1_700_000_000);

        assertThat(location.formatToAttribute(), is("France:Paris:1700000000"));
    }

    @Test
    void formatsLegacyWithoutTimestamp() {
        var location = KnownLocationData.parseFromAttribute("France:Paris");

        assertThat(location.formatToAttribute(), is("France:Paris"));
    }

    @Test
    void parsesZeroTimestampAsUndated() {
        var location = KnownLocationData.parseFromAttribute("France:Paris:0");

        assertThat(location, notNullValue());
        assertThat(location.lastSeenEpochSeconds(), nullValue());
        assertThat(location.isExpired(1_700_000_000, 90), is(false));
    }

    @Test
    void detectsExpiredLocation() {
        int now = 1_700_000_000;
        var location = KnownLocationData.of("France", "Paris", now - (int) Duration.ofDays(91).toSeconds());

        assertThat(location.isExpired(now, 90), is(true));
    }

    @Test
    void keepsFreshLocation() {
        int now = 1_700_000_000;
        var location = KnownLocationData.of("France", "Paris", now - (int) Duration.ofDays(30).toSeconds());

        assertThat(location.isExpired(now, 90), is(false));
    }

    @Test
    void neverExpiresWhenTtlDisabled() {
        int now = 1_700_000_000;
        var location = KnownLocationData.of("France", "Paris", now - (int) Duration.ofDays(365).toSeconds());

        assertThat(location.isExpired(now, 0), is(false));
    }

    @Test
    void ensureLastSeen_backfillsUndatedEntriesOnly() {
        int now = 1_700_000_000;
        var legacy = KnownLocationData.parseFromAttribute("France:Paris");
        var dated = KnownLocationData.of("Germany", "Berlin", now - 100);

        assertThat(legacy.isUndated(), is(true));
        assertThat(legacy.ensureLastSeen(now).lastSeenEpochSeconds(), is(now));
        assertThat(dated.isUndated(), is(false));
        assertThat(dated.ensureLastSeen(now), is(dated));
    }

    @Test
    void matchesCountryAndCity() {
        var location = KnownLocationData.of("France", "Paris", 1);

        assertThat(location.matches("France", "Paris"), is(true));
        assertThat(location.matches("France", "Lyon"), is(false));
    }
}
