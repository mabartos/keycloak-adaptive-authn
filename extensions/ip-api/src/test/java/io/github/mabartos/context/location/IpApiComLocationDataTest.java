package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;
import org.keycloak.util.JsonSerialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IpApiComLocationDataTest {

    @Test
    void parsesSuccessResponse() throws Exception {
        String json = """
                {
                  "status": "success",
                  "country": "France",
                  "region": "IDF",
                  "regionName": "Île-de-France",
                  "city": "Paris",
                  "zip": "75001",
                  "lat": 48.8566,
                  "lon": 2.3522,
                  "timezone": "Europe/Paris"
                }
                """;

        IpApiComLocationData data = JsonSerialization.readValue(json, IpApiComLocationData.class);

        assertThat(data.isSuccess(), is(true));
        assertThat(data.getCountry(), is("France"));
        assertThat(data.getRegionCode(), is("IDF"));
        assertThat(data.getRegion(), is("Île-de-France"));
        assertThat(data.getCity(), is("Paris"));
        assertThat(data.getPostalCode(), is("75001"));
        assertThat(data.getLatitude(), is(48.8566));
        assertThat(data.getLongitude(), is(2.3522));
        assertThat(data.getTimezone(), is("Europe/Paris"));
        assertThat(data.getContinent(), nullValue());
        assertThat(data.getCurrency(), nullValue());
    }

    @Test
    void parsesFailResponse() throws Exception {
        String json = """
                {"status": "fail", "message": "private range"}
                """;

        IpApiComLocationData data = JsonSerialization.readValue(json, IpApiComLocationData.class);

        assertThat(data.isSuccess(), is(false));
        assertThat(data.getStatusMessage(), is("private range"));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(Exception.class, () -> JsonSerialization.readValue("{not json", IpApiComLocationData.class));
    }
}
