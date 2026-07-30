package io.github.mabartos.context.location.maxmind;

import com.maxmind.geoip2.DatabaseReader;
import io.github.mabartos.context.location.LocationData;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class MaxMindLocationDataTest {

    @Test
    void from_mapsCityResponseToLocationData() throws Exception {
        MaxMindTestFixtures.assumeTestDatabasePresent();

        try (DatabaseReader reader =
                new DatabaseReader.Builder(MaxMindTestFixtures.testDatabasePath().toFile()).build()) {
            var response = reader.city(InetAddress.getByName("81.2.69.160"));
            LocationData data = MaxMindLocationData.from(response).orElseThrow();

            assertThat(data.getCountry(), is("United Kingdom"));
            assertThat(data.getCity(), is("London"));
            assertThat(data.getRegion(), is("England"));
            assertThat(data.getRegionCode(), is("ENG"));
            assertThat(data.getContinent(), is("EU"));
            assertThat(data.getLatitude(), notNullValue());
            assertThat(data.getLongitude(), notNullValue());
            assertThat(data.getTimezone(), is("Europe/London"));
            assertThat(data.getCurrency(), is((String) null));
        }
    }
}
