package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class LocationDataTest {

    @Test
    public void testLocationDataCreation() {
        TestLocationData location = new TestLocationData(
            "United States",
            "US",
            "California",
            "San Francisco",
            "94102",
            37.7749,
            -122.4194,
            "America/Los_Angeles"
        );

        assertThat(location, notNullValue());
        assertThat(location.getCountry(), is("United States"));
        assertThat(location.getCountryCode(), is("US"));
        assertThat(location.getRegion(), is("California"));
        assertThat(location.getCity(), is("San Francisco"));
        assertThat(location.getPostalCode(), is("94102"));
        assertThat(location.getLatitude(), is(37.7749));
        assertThat(location.getLongitude(), is(-122.4194));
        assertThat(location.getTimezone(), is("America/Los_Angeles"));
    }

    @Test
    public void testLocationDataWithNulls() {
        TestLocationData location = new TestLocationData(
            "Unknown",
            null,
            null,
            null,
            null,
            0.0,
            0.0,
            null
        );

        assertThat(location.getCountry(), is("Unknown"));
        assertThat(location.getCountryCode(), is((String) null));
        assertThat(location.getRegion(), is((String) null));
    }

    @Test
    public void testLocationDataDifferentCountries() {
        TestLocationData us = new TestLocationData("United States", "US", null, null, null, 0, 0, null);
        TestLocationData uk = new TestLocationData("United Kingdom", "GB", null, null, null, 0, 0, null);

        assertThat(us.getCountryCode(), is("US"));
        assertThat(uk.getCountryCode(), is("GB"));
        assertThat(us.getCountryCode().equals(uk.getCountryCode()), is(false));
    }

    @Test
    public void testLocationDataCoordinates() {
        TestLocationData newYork = new TestLocationData(
            "United States", "US", "New York", "New York", "10001",
            40.7128, -74.0060, "America/New_York"
        );

        TestLocationData london = new TestLocationData(
            "United Kingdom", "GB", "England", "London", "SW1A",
            51.5074, -0.1278, "Europe/London"
        );

        assertThat(newYork.getLatitude() != london.getLatitude(), is(true));
        assertThat(newYork.getLongitude() != london.getLongitude(), is(true));
    }

    @Test
    public void testLocationDataEqualsBasedOnCountryCode() {
        TestLocationData location1 = new TestLocationData("France", "FR", null, null, null, 0, 0, null);
        TestLocationData location2 = new TestLocationData("France", "FR", null, null, null, 0, 0, null);
        TestLocationData location3 = new TestLocationData("Germany", "DE", null, null, null, 0, 0, null);

        assertThat(location1.getCountryCode().equals(location2.getCountryCode()), is(true));
        assertThat(location1.getCountryCode().equals(location3.getCountryCode()), is(false));
    }

    // Test implementation of LocationData
    static class TestLocationData implements LocationData {
        private final String country;
        private final String countryCode;
        private final String region;
        private final String city;
        private final String postalCode;
        private final Double latitude;
        private final Double longitude;
        private final String timezone;

        TestLocationData(String country, String countryCode, String region, String city,
                        String postalCode, double latitude, double longitude, String timezone) {
            this.country = country;
            this.countryCode = countryCode;
            this.region = region;
            this.city = city;
            this.postalCode = postalCode;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = timezone;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public String getRegion() {
            return region;
        }

        @Override
        public String getRegionCode() {
            return countryCode;
        }

        @Override
        public String getCountry() {
            return country;
        }

        @Override
        public String getContinent() {
            return null;
        }

        @Override
        public String getPostalCode() {
            return postalCode;
        }

        @Override
        public Double getLatitude() {
            return latitude;
        }

        @Override
        public Double getLongitude() {
            return longitude;
        }

        @Override
        public String getTimezone() {
            return timezone;
        }

        @Override
        public String getCurrency() {
            return null;
        }

        public String getCountryCode() {
            return countryCode;
        }
    }
}
