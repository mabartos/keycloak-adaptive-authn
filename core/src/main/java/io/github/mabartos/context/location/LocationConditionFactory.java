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

import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.UserContextConditionFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.stream.Stream;

public class LocationConditionFactory extends UserContextConditionFactory<LocationContext> {
    public static final String PROVIDER_ID = "location-conditional-authenticator";

    public static final String CONTINENT_LIST_CONFIG = "continentListConfig";
    public static final String CONTINENT_VALUE_CONFIG = "continentValueConfig";

    public static final String COUNTRY_LIST_CONFIG = "countryListConfig";
    public static final String COUNTRY_VALUE_CONFIG = "countryValueConfig";

    public static final String CITY_LIST_CONFIG = "cityListConfig";
    public static final String CITY_VALUE_CONFIG = "cityValueConfig";

    public static final Operation<LocationContext> CONTINENT_IS = new Operation<>("CONT_EQ", "continent is", (location, val) -> location.getData().map(LocationData::getContinent).filter(f -> f.equals(val)).isPresent());
    public static final Operation<LocationContext> CONTINENT_IS_NOT = new Operation<>("CONT_NEQ", "continent is not", (location, val) -> location.getData().map(LocationData::getContinent).filter(f -> f.equals(val)).isEmpty());

    public static final Operation<LocationContext> COUNTRY_IS = new Operation<>("COUNTRY_EQ", "country is", (location, val) -> location.getData().map(LocationData::getCountry).filter(f -> f.equals(val)).isPresent());
    public static final Operation<LocationContext> COUNTRY_IS_NOT = new Operation<>("COUNTRY_NEQ", "country is not", (location, val) -> location.getData().map(LocationData::getContinent).filter(f -> f.equals(val)).isEmpty());

    public static final Operation<LocationContext> CITY_IS = new Operation<>("CITY_EQ", "city is", (location, val) -> location.getData().map(LocationData::getCity).filter(f -> f.equals(val)).isPresent());
    public static final Operation<LocationContext> CITY_IS_NOT = new Operation<>("CITY_NEQ", "city is not", (location, val) -> location.getData().map(LocationData::getContinent).filter(f -> f.equals(val)).isEmpty());

    public LocationConditionFactory() {
    }

    @Override
    public LocationCondition create(KeycloakSession session) {
        return new LocationCondition(session, getOperations());
    }

    @Override
    public List<Operation<LocationContext>> initOperations() {
        return List.of(CONTINENT_IS, CONTINENT_IS_NOT, COUNTRY_IS, COUNTRY_IS_NOT, CITY_IS, CITY_IS_NOT);
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
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                // Continent
                .property()
                .name(CONTINENT_LIST_CONFIG)
                .options(Stream.of(CONTINENT_IS, CONTINENT_IS_NOT).map(Operation::getText).toList())
                .label(CONTINENT_LIST_CONFIG)
                .helpText(CONTINENT_LIST_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()
                .property()
                .name(CONTINENT_VALUE_CONFIG)
                .label(CONTINENT_VALUE_CONFIG)
                .helpText(CONTINENT_VALUE_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("")
                .add()
                // Country
                .property()
                .name(COUNTRY_LIST_CONFIG)
                .options(Stream.of(COUNTRY_IS, COUNTRY_IS_NOT).map(Operation::getText).toList())
                .label(COUNTRY_LIST_CONFIG)
                .helpText(COUNTRY_LIST_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()
                .property()
                .name(COUNTRY_VALUE_CONFIG)
                .label(COUNTRY_VALUE_CONFIG)
                .helpText(COUNTRY_VALUE_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("")
                .add()
                // City
                .property()
                .name(CITY_LIST_CONFIG)
                .options(Stream.of(CITY_IS, CITY_IS_NOT).map(Operation::getText).toList())
                .label(CITY_LIST_CONFIG)
                .helpText(CITY_LIST_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()
                .property()
                .name(CITY_VALUE_CONFIG)
                .label(CITY_VALUE_CONFIG)
                .helpText(CITY_VALUE_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("")
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
