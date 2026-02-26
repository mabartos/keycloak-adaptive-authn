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
package io.github.mabartos.evaluator.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.location.IpApiLocationContext;
import io.github.mabartos.context.location.IpApiLocationContextFactory;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.mabartos.context.ip.client.TestIpAddressContextFactory.USE_TESTING_IP_PROP;

/**
 * Risk evaluator for location properties
 */
public class LocationRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(LocationRiskEvaluator.class);
    private static final String KNOWN_LOCATIONS_ATTR = "adaptive_authn.known_locations";
    private static final String LOCATION_SEPARATOR = ";";
    private static final int MAX_STORED_LOCATIONS = 10;

    private final IpApiLocationContext locationContext;

    public LocationRiskEvaluator(KeycloakSession session) {
        this.locationContext = UserContexts.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var currentLocation = locationContext.getData(realm, knownUser).orElse(null);
        if (currentLocation == null) {
            logger.tracef("Cannot obtain current location data");
            return Risk.invalid("Cannot obtain location information");
        }

        logger.tracef("Current location: %s", currentLocation.toString());

        // Get known locations for this user
        List<String> knownLocations = getKnownLocations(knownUser);

        if (knownLocations.isEmpty()) {
            logger.tracef("No known locations for user. This is a new user or first login tracking.");
            return Risk.of(Risk.SMALL, "First tracked location");
        }

        // Calculate risk based on location match
        double risk = calculateLocationRisk(currentLocation, knownLocations);

        return Risk.of(risk);
    }

    protected double calculateLocationRisk(LocationData currentLocation, List<String> knownLocations) {
        String currentLocationKey = getLocationKey(currentLocation);

        // Check if this exact location (city + country) has been seen before
        boolean exactMatch = knownLocations.contains(currentLocationKey);
        if (exactMatch) {
            return Risk.NONE;
        }

        // Check if same country has been seen before
        boolean sameCountry = knownLocations.stream()
                .anyMatch(loc -> loc.endsWith(":" + currentLocation.getCountry()));
        if (sameCountry) {
            return Risk.SMALL;
        }

        // Completely new country
        return Risk.INTERMEDIATE;
    }

    protected List<String> getKnownLocations(UserModel user) {
        List<String> locations = user.getAttributeStream(KNOWN_LOCATIONS_ATTR)
                .collect(Collectors.toList());

        if (locations.isEmpty()) {
            return Collections.emptyList();
        }

        // Parse stored locations
        return Arrays.stream(locations.get(0).split(LOCATION_SEPARATOR))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void saveSuccessfulLoginLocation(UserModel user, LocationData location) {
        if (location == null) {
            return;
        }

        String locationKey = getLocationKey(location);
        List<String> knownLocations = getKnownLocations(user);

        // Add new location if not already present
        if (!knownLocations.contains(locationKey)) {
            knownLocations.add(locationKey);

            // Keep only the last N locations
            if (knownLocations.size() > MAX_STORED_LOCATIONS) {
                knownLocations = knownLocations.subList(
                        knownLocations.size() - MAX_STORED_LOCATIONS,
                        knownLocations.size()
                );
            }

            // Store back to user attributes
            String joined = String.join(LOCATION_SEPARATOR, knownLocations);
            user.setSingleAttribute(KNOWN_LOCATIONS_ATTR, joined);
            logger.tracef("Saved location for user %s: %s", user.getUsername(), locationKey);
        }
    }

    protected String getLocationKey(LocationData location) {
        return location.getCity() + ":" + location.getCountry();
    }

    protected String getUserLocationKey(UserModel user) {
        return "location-" + user.getId();
    }
}
