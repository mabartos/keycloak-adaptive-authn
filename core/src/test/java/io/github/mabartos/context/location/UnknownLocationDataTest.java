package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class UnknownLocationDataTest {

    @Test
    void allStringFieldsAreUnknown() {
        LocationData u = UnknownLocationData.INSTANCE;
        assertThat(u.getCity(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getRegion(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getRegionCode(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getCountry(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getContinent(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getPostalCode(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getTimezone(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getCurrency(), is(UnknownLocationData.UNKNOWN));
        assertThat(u.getLatitude(), nullValue());
        assertThat(u.getLongitude(), nullValue());
    }
}
