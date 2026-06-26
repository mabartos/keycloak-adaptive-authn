package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;
import org.keycloak.util.JsonSerialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IpApiLocationDataTest {

    @Test
    void parsesSuccessResponse() throws Exception {
        String json = """
                {
                  "city": "Paris",
                  "region": "Île-de-France",
                  "region_code": "IDF",
                  "country_name": "France",
                  "country_capital": "Paris",
                  "continent_code": "EU",
                  "postal": "75001",
                  "latitude": 48.8566,
                  "longitude": 2.3522,
                  "timezone": "Europe/Paris",
                  "currency": "EUR"
                }
                """;

        IpApiLocationData data = JsonSerialization.readValue(json, IpApiLocationData.class);

        assertThat(data.getCountry(), is("France"));
        assertThat(data.getRegionCode(), is("IDF"));
        assertThat(data.getRegion(), is("Île-de-France"));
        assertThat(data.getCity(), is("Paris"));
        assertThat(data.getPostalCode(), is("75001"));
        assertThat(data.getLatitude(), is(48.8566));
        assertThat(data.getLongitude(), is(2.3522));
        assertThat(data.getTimezone(), is("Europe/Paris"));
        assertThat(data.getCurrency(), is("EUR"));
        assertThat(data.getContinent(), is("EU"));
    }

    @Test
    void getCountryReturnsNullWhenCountryNameAbsent() throws Exception {
        IpApiLocationData data = JsonSerialization.readValue("{\"city\":\"Paris\"}", IpApiLocationData.class);

        assertThat(data.getCountry(), nullValue());
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(Exception.class, () -> JsonSerialization.readValue("{not json", IpApiLocationData.class));
    }
}
