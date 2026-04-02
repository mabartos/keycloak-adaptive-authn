package io.github.mabartos.context.location;

import io.github.mabartos.context.location.LocationCountryOperationTest.StaticLocationContext;
import io.github.mabartos.context.location.LocationCountryOperationTest.StaticLocationData;
import io.github.mabartos.spi.condition.Operation;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for country-based operations COUNTRY_IS and COUNTRY_IS_NOT.
 */
public class LocationCountryOperationTest {

    /* Tests 'COUNTRY_IS' single-value */
    @Test
    public void testCountryIsSingleValueMatches() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.COUNTRY_IS.match(realm, context, "France");

        assertThat(result, is(true));
    }

    @Test
    public void testCountryIsSingleValueDontMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.COUNTRY_IS.match(realm, context, "Germany");

        assertThat(result, is(false));
    }

    /* Tests 'COUNTRY_IS' multi-values */
    @Test
    public void testCountryIsMultiValuesMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Germany" + delimiter + "France" + delimiter + "Spain";

        boolean result = LocationConditionFactory.COUNTRY_IS.match(realm, context, values);

        assertThat(result, is(true));
    }

    @Test
    public void testCountryIsMultiValuesDontMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Germany" + delimiter + "Spain";

        boolean result = LocationConditionFactory.COUNTRY_IS.match(realm, context, values);

        assertThat(result, is(false));
    }

    @Test
    public void testCountryIsMultiValuesTrimmedMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Germany" + delimiter + " France " + delimiter + "Spain";

        boolean result = LocationConditionFactory.COUNTRY_IS.match(realm, context, values);

        assertThat(result, is(true));
    }

    /* Tests 'COUNTRY_IS_NOT' single-value */
    @Test
    public void testCountryIsNotSingleValueMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, "France");

        assertThat(result, is(false));
    }
    
    @Test
    public void testCountryIsNotSingleValueDontMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, "Germany");

        assertThat(result, is(true));
    }

    /* Tests 'COUNTRY_IS_NOT' multi-values */
    @Test
    public void testCountryIsNotMultiValuesMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Germany" + delimiter + "France";

        boolean result = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, values);

        assertThat(result, is(false));
    }
    
    @Test
    public void testCountryIsNotMultiValuesDontMatch() {
        var context = new StaticLocationContext("France");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Germany" + delimiter + "Spain";

        boolean result = LocationConditionFactory.COUNTRY_IS_NOT.match(realm, context, values);

        assertThat(result, is(true));
    }

    /**
     * Simple {@link LocationContext} implementation for tests that always returns
     * a static {@link LocationData} instance with the given country.
     */
    static class StaticLocationContext extends LocationContext {

        private final LocationData data;

        StaticLocationContext(String country) {
            super((KeycloakSession) null);
            this.data = new StaticLocationData(country);
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

    /**
     * Simple {@link LocationData} implementation for tests.
     */
    static class StaticLocationData implements LocationData {

        private final String country;

        StaticLocationData(String country) {
            this.country = country;
        }

        @Override
        public String getCity() {
            return null;
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