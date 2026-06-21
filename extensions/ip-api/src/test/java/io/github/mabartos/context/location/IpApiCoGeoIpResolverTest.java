package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;
import org.keycloak.util.JsonSerialization;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IpApiCoGeoIpResolverTest {

    @Test
    void toLocationOrEmpty_returnsDataWhenCountryNamePresent() throws Exception {
        IpApiLocationData data = JsonSerialization.readValue(
                """
                {"country_name":"Germany","city":"Berlin"}
                """,
                IpApiLocationData.class);

        Optional<LocationData> location = IpApiCoGeoIpResolver.toLocationOrEmpty(data);

        assertThat(location.isPresent(), is(true));
        assertThat(location.get().getCountry(), is("Germany"));
        assertThat(location.get().getCity(), is("Berlin"));
    }

    @Test
    void toLocationOrEmpty_returnsEmptyForNullPayload() {
        assertThat(IpApiCoGeoIpResolver.toLocationOrEmpty(null).isEmpty(), is(true));
    }

    @Test
    void toLocationOrEmpty_returnsEmptyWhenCountryNameBlank() throws Exception {
        IpApiLocationData data = JsonSerialization.readValue(
                """
                {"country_name":"  ","city":"Paris"}
                """,
                IpApiLocationData.class);

        assertThat(IpApiCoGeoIpResolver.toLocationOrEmpty(data).isEmpty(), is(true));
    }

    @Test
    void toLocationOrEmpty_returnsEmptyWhenCountryNameMissing() throws Exception {
        IpApiLocationData data = JsonSerialization.readValue(
                """
                {"city":"Paris"}
                """,
                IpApiLocationData.class);

        assertThat(IpApiCoGeoIpResolver.toLocationOrEmpty(data).isEmpty(), is(true));
    }
}
