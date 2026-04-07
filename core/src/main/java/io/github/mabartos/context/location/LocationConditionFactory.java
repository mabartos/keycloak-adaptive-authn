/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.context.location;

import io.github.mabartos.spi.condition.DefaultOperation;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.UserContextConditionFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.stream.Stream;

public class LocationConditionFactory extends UserContextConditionFactory<LocationContext> {
    public static final String PROVIDER_ID = "location-conditional-authenticator";

    public static final String COUNTRY_LIST_CONFIG = "countryListConfig";
    public static final String COUNTRY_VALUE_CONFIG = "countryValueConfig";

    public static final String CITY_LIST_CONFIG = "cityListConfig";
    public static final String CITY_VALUE_CONFIG = "cityValueConfig";

    public static final Operation<LocationContext> COUNTRY_IS = new Operation<>(
            "COUNTRY_" + DefaultOperation.ANY_OF.symbol(),
            "country is",
            LocationConditionFactory::matchesAnyCountry,
            true);
    public static final Operation<LocationContext> COUNTRY_IS_NOT = new Operation<>(
            "COUNTRY_" + DefaultOperation.NONE_OF.symbol(),
            "country is not",
            (realm, location, vals) -> !matchesAnyCountry(realm, location, vals),
            true);

    public static final Operation<LocationContext> CITY_IS = new Operation<>(
            "CITY_" + DefaultOperation.ANY_OF.symbol(),
            "city is",
            LocationConditionFactory::matchesAnyCity,
            true);
    public static final Operation<LocationContext> CITY_IS_NOT = new Operation<>(
            "CITY_" + DefaultOperation.NONE_OF.symbol(),
            "city is not",
            (realm, location, vals) -> !matchesAnyCity(realm, location, vals),
            true);


    public LocationConditionFactory() {
    }

    private static boolean matchesAnyCountry(org.keycloak.models.RealmModel realm, LocationContext location, List<String> countries) {
        String detectedCountry = location.getData(realm).map(LocationData::getCountry).orElse("<unknown>");
        return normalizeValues(countries).stream().anyMatch(detectedCountry::equals);
    }

    private static boolean matchesAnyCity(org.keycloak.models.RealmModel realm, LocationContext location, List<String> cities) {
        String detectedCity = location.getData(realm).map(LocationData::getCity).orElse("<unknown>");
        return normalizeValues(cities).stream().anyMatch(detectedCity::equals);
    }

    private static List<String> normalizeValues(List<String> values) {
        return values.stream()
                .map(value -> value != null ? value.trim() : null)
                .filter(StringUtil::isNotBlank)
                .toList();
    }

    @Override
    public LocationCondition create(KeycloakSession session) {
        return new LocationCondition(session, getOperations());
    }

    @Override
    public List<Operation<LocationContext>> initOperations() {
        return List.of(COUNTRY_IS, COUNTRY_IS_NOT, CITY_IS, CITY_IS_NOT);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                // Country
                .property()
                .name(COUNTRY_LIST_CONFIG)
                .options(Stream.of(COUNTRY_IS, COUNTRY_IS_NOT).map(Operation::getText).toList())
                .label("Country condition")
                .helpText("whether the user's country must match or must not match the configured values.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()

                .property()
                .name(COUNTRY_VALUE_CONFIG)
                .label("Countries")
                .helpText("Enter one or more country names, for example: France, Germany, Switzerland ...")
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .defaultValue(List.of())
                .add()

                // City
                .property()
                .name(CITY_LIST_CONFIG)
                .options(Stream.of(CITY_IS, CITY_IS_NOT).map(Operation::getText).toList())
                .label("City condition")
                .helpText("Defines whether the user's city must match or must not match the configured value.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()

                .property()
                .name(CITY_VALUE_CONFIG)
                .label("City")
                .helpText("Enter one or more city names, for example: Paris, Berlin, Zurich ...")
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .defaultValue(List.of())
                .add()

                .build();
    }

    @Override
    public String getDisplayType() {
        return "Condition - Location";
    }

    @Override
    public String getHelpText() {
        return "Condition matching Location attributes";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
