package io.github.mabartos.context.location;

import io.github.mabartos.spi.condition.Operation;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for city-based operations CITY_IS and CITY_IS_NOT.
 */
public class LocationCityOperationTest {

    /* Tests 'CITY_IS' single-value */
    @Test
    public void testCityIsSingleValueMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.CITY_IS.match(realm, context, "Paris");

        assertThat(result, is(true));
    }

    @Test
    public void testCityIsSingleValueDontMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.CITY_IS.match(realm, context, "Lyon");

        assertThat(result, is(false));
    }

    /* Tests 'CITY_IS' multi-values */
    @Test
    public void testCityIsMultiValuesMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Lyon" + delimiter + "Paris" + delimiter + "Marseille";

        boolean result = LocationConditionFactory.CITY_IS.match(realm, context, values);

        assertThat(result, is(true));
    }

    @Test
    public void testCityIsMultiValuesDontMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Lyon" + delimiter + "Marseille";

        boolean result = LocationConditionFactory.CITY_IS.match(realm, context, values);

        assertThat(result, is(false));
    }

    @Test
    public void testCityIsMultiValuesTrimmedMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Lyon" + delimiter + " Paris " + delimiter + "Marseille";

        boolean result = LocationConditionFactory.CITY_IS.match(realm, context, values);

        assertThat(result, is(true));
    }

    /* Tests 'CITY_IS_NOT' single-value */
    @Test
    public void testCityIsNotSingleValueMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.CITY_IS_NOT.match(realm, context, "Lyon");

        assertThat(result, is(true));
    }

    @Test
    public void testCityIsNotSingleValuesDontMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        boolean result = LocationConditionFactory.CITY_IS_NOT.match(realm, context, "Paris");

        assertThat(result, is(false));
    }

    /* Tests 'CITY_IS_NOT' multi-values */
    @Test
    public void testCityIsNotMultiValuesMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Lyon" + delimiter + "Marseille";

        boolean result = LocationConditionFactory.CITY_IS_NOT.match(realm, context, values);

        assertThat(result, is(true));
    }

    @Test
    public void testCityIsNotMultiValuesDontMatch() {
        var context = new StaticLocationContext("Paris");
        RealmModel realm = null;

        String delimiter = Operation.DEFAULT_MULTI_VALUES_DELIMITER;
        String values = "Lyon" + delimiter + "Paris";

        boolean result = LocationConditionFactory.CITY_IS_NOT.match(realm, context, values);

        assertThat(result, is(false));
    }

    /**
     * Simple {@link LocationContext} implementation for tests that always returns
     * a static {@link LocationData} instance with the given city.
     */
    static class StaticLocationContext extends LocationContext {

        private final LocationData data;

        StaticLocationContext(String city) {
            super((KeycloakSession) null);
            this.data = new StaticLocationData(city);
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

        private final String city;

        StaticLocationData(String city) {
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
            return null;
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