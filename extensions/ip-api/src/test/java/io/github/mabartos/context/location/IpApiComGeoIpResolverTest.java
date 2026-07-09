package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;
import org.keycloak.util.JsonSerialization;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IpApiComGeoIpResolverTest {

    @Test
    void toLocationOrEmpty_returnsDataForSuccessfulResponse() throws Exception {
        IpApiComLocationData data = JsonSerialization.readValue(
                """
                {"status":"success","country":"Germany","city":"Berlin"}
                """,
                IpApiComLocationData.class);

        Optional<LocationData> location = IpApiComGeoIpResolver.toLocationOrEmpty(data);

        assertThat(location.isPresent(), is(true));
        assertThat(location.get().getCountry(), is("Germany"));
        assertThat(location.get().getCity(), is("Berlin"));
    }

    @Test
    void toLocationOrEmpty_returnsEmptyForFailStatus() throws Exception {
        IpApiComLocationData data = JsonSerialization.readValue(
                """
                {"status":"fail","message":"invalid query"}
                """,
                IpApiComLocationData.class);

        assertThat(IpApiComGeoIpResolver.toLocationOrEmpty(data).isEmpty(), is(true));
    }

    @Test
    void toLocationOrEmpty_returnsEmptyForNullPayload() {
        assertThat(IpApiComGeoIpResolver.toLocationOrEmpty(null).isEmpty(), is(true));
    }

    @Test
    void toLocationOrEmpty_returnsEmptyWhenCountryBlank() throws Exception {
        IpApiComLocationData data = JsonSerialization.readValue(
                """
                {"status":"success","country":"  ","city":"Paris"}
                """,
                IpApiComLocationData.class);

        assertThat(IpApiComGeoIpResolver.toLocationOrEmpty(data).isEmpty(), is(true));
    }
}
