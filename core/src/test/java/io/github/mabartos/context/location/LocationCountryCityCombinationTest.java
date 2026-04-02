package io.github.mabartos.context.location;

import io.github.mabartos.spi.condition.Operation;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for combined evaluation of country + city operations.
 *
 * This mirrors how {@link LocationCondition} effectively evaluates config:
 * country must match, and if city is specified, it must also match.
 */
public class LocationCountryCityCombinationTest {

    /* '*_IS' single-value*/
    @Test
    public void testCountryAndCitySingleValueMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, "France");
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, "Paris");

        assertThat(countryMatches && cityMatches, is(true));
    }

    @Test
    public void testCountryMatchCityDontMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, "France");
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, "Lyon");

        assertThat(countryMatches && cityMatches, is(false));
    }

    @Test
    public void testCountryDontMatchCityMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, "Germany");
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, "Paris");

        assertThat(countryMatches && cityMatches, is(false));
    }

    /* '*_IS' multi-values */
    @Test
    public void testCountryAndCityMultiValuesMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "France" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Paris" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(true));
    }

    @Test
    public void testCountryMultiValuesMatchCityMultiValuesDontMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "France" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(false));
    }

    @Test
    public void testCountryMultiValuesDontMatchCityMultiValuesMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Paris" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(false));
    }

    /* '*_IS_NOT' single-value */
    @Test
    public void testCountryAndCityIsNotSingleValueMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, "Germany");
        boolean cityMatches = LocationConditionFactory.CITY_IS_NOT.match(realm, context, "Lyon");

        assertThat(countryMatches && cityMatches, is(true));
    }

    @Test
    public void testCountryIsNotMatchCityIsNotDontMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, "Germany");
        boolean cityMatches = LocationConditionFactory.CITY_IS_NOT.match(realm, context, "Paris");

        assertThat(countryMatches && cityMatches, is(false));
    }

    @Test
    public void testCountryIsNotDontMatchCityIsNotMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, "France");
        boolean cityMatches = LocationConditionFactory.CITY_IS_NOT.match(realm, context, "Lyon");

        assertThat(countryMatches && cityMatches, is(false));
    }

    /* '*_IS_NOT' multi-values */
    @Test
    public void testCountryAndCityIsNotMultiValuesMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS_NOT.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(true));
    }

    @Test
    public void testCountryIsNotMultiValuesMatchCityIsNotMultiValuesDontMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Paris" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS_NOT.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(false));
    }

    @Test
    public void testCountryIsNotMultiValuesDontMatchCityIsNotMultiValuesMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "France" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS_NOT.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(false));
    }

    /* Mix '*_IS'/'*_IS_NOT' single-value*/
    @Test
    public void testCountryIsNotMatchAndCityIsMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, "Germany");
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, "Paris");

        assertThat(countryMatches && cityMatches, is(true));
    }

    @Test
    public void testCountryMultiValuesDontMatchCitySingleValueMatch() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "France";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, "Paris");

        assertThat(countryMatches && cityMatches, is(false));
    }

    @Test
    public void testCountryAndCityBothMultiValueMatches() {
        var context = new StaticLocationContext("France", "Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String countryValues = "Germany" + delimiter + "France" + delimiter + "Spain";
        String cityValues = "Lyon" + delimiter + "Paris" + delimiter + "Marseille";

        boolean countryMatches = LocationConditionFactory.COUNTRY_IS.match(realm, context, countryValues);
        boolean cityMatches = LocationConditionFactory.CITY_IS.match(realm, context, cityValues);

        assertThat(countryMatches && cityMatches, is(true));
    }

    /**
     * Simple {@link LocationContext} implementation for tests that always returns
     * a static {@link LocationData} instance with the given country and city.
     *
     * Overrides getData to avoid requiring a real {@link KeycloakSession}.
     */
    static class StaticLocationContext extends LocationContext {

        private final LocationData data;

        StaticLocationContext(String country, String city) {
            super((KeycloakSession) null);
            this.data = new StaticLocationData(country, city);
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public Optional<LocationData> initData(RealmModel realm) {
            return Optional.of(data);
        }

        @Override
        public Optional<LocationData> getData(RealmModel realm) {
            return Optional.of(data);
        }

        @Override
        public Optional<LocationData> getData(RealmModel realm, org.keycloak.models.UserModel knownUser) {
            return Optional.of(data);
        }
    }

    static class StaticLocationData implements LocationData {
        private final String country;
        private final String city;

        StaticLocationData(String country, String city) {
            this.country = country;
            this.city = city;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public String getRegion() {
            return null;
        }

        @Override
        public String getRegionCode() {
            return null;
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
            return null;
        }

        @Override
        public Double getLatitude() {
            return null;
        }

        @Override
        public Double getLongitude() {
            return null;
        }

        @Override
        public String getTimezone() {
            return null;
        }

        @Override
        public String getCurrency() {
            return null;
        }
    }
}