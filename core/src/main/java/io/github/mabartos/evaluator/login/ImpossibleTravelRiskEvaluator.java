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
package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.location.IpApiLocationContext;
import io.github.mabartos.context.location.IpApiLocationContextFactory;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.util.Comparator;
import java.util.Set;

/**
 * Risk evaluator for detecting logins from locations that are physically
 * impossible to reach in the time between logins
 */
public class ImpossibleTravelRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(ImpossibleTravelRiskEvaluator.class);

    // Maximum realistic travel speed in km/h (commercial aircraft ~900 km/h + buffer)
    private static final double MAX_TRAVEL_SPEED_KMH = 1000.0;

    // Minimum time difference to consider (to avoid false positives for local movements)
    private static final long MIN_TIME_DIFF_MINUTES = 30;

    private static final String LAST_LOGIN_LOCATION_ATTR = "adaptive_authn.last_login_location";
    private static final String LAST_LOGIN_TIME_ATTR = "adaptive_authn.last_login_time";

    private final KeycloakSession session;
    private final IpApiLocationContext locationContext;
    private final LoginEventsContext loginEventsContext;

    public ImpossibleTravelRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.locationContext = UserContexts.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
        this.loginEventsContext = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.VERY_IMPORTANT;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var currentLocation = locationContext.getData(realm, knownUser).orElse(null);
        if (currentLocation == null || currentLocation.getLatitude() == null || currentLocation.getLongitude() == null) {
            logger.tracef("No current location data available");
            return Risk.invalid("Cannot obtain current location");
        }

        // Get the most recent successful login event
        var lastLoginInfo = getLastLoginInfo(realm, knownUser);
        if (lastLoginInfo == null) {
            // First login or no previous data, save current location
            saveLoginLocation(knownUser, currentLocation);
            return Risk.notEnoughInfo("No previous login location");
        }

        var lastLocation = lastLoginInfo.location;
        var lastLoginTime = lastLoginInfo.time;

        if (lastLocation.getLatitude() == null || lastLocation.getLongitude() == null) {
            logger.tracef("Last location missing coordinates");
            return Risk.invalid("Last location missing coordinates");
        }

        long currentTime = Time.currentTimeMillis();
        long timeDiffMinutes = (currentTime - lastLoginTime) / 1000 / 60;

        // Skip check if too little time has passed (avoid false positives)
        if (timeDiffMinutes < MIN_TIME_DIFF_MINUTES) {
            logger.tracef("Time difference too small: %d minutes", timeDiffMinutes);
            return Risk.none();
        }

        // Calculate distance between locations
        double distanceKm = calculateDistance(
                lastLocation.getLatitude(), lastLocation.getLongitude(),
                currentLocation.getLatitude(), currentLocation.getLongitude()
        );

        // Calculate required speed
        double requiredSpeedKmh = distanceKm / (timeDiffMinutes / 60.0);

        logger.tracef("Distance: %.2f km, Time: %d min, Required speed: %.2f km/h",
                distanceKm, timeDiffMinutes, requiredSpeedKmh);

        // Save current location for next check
        saveLoginLocation(knownUser, currentLocation);

        // Evaluate risk based on required speed
        if (requiredSpeedKmh > MAX_TRAVEL_SPEED_KMH) {
            return Risk.of(Risk.HIGHEST, String.format(
                    "Impossible travel: %.0f km in %d minutes (%.0f km/h required)",
                    distanceKm, timeDiffMinutes, requiredSpeedKmh
            ));
        } else if (requiredSpeedKmh > 800) {
            // Very fast but theoretically possible (intercontinental flight)
            return Risk.of(Risk.INTERMEDIATE, String.format(
                    "Very fast travel: %.0f km in %d minutes",
                    distanceKm, timeDiffMinutes
            ));
        } else if (requiredSpeedKmh > 500) {
            // Fast travel (likely flight)
            return Risk.of(Risk.SMALL, "Fast travel detected");
        }

        return Risk.none();
    }

    private LastLoginInfo getLastLoginInfo(RealmModel realm, UserModel user) {
        // Try to get from user attributes first
        var lastLatStr = user.getFirstAttribute(LAST_LOGIN_LOCATION_ATTR + ".lat");
        var lastLonStr = user.getFirstAttribute(LAST_LOGIN_LOCATION_ATTR + ".lon");
        var lastTimeStr = user.getFirstAttribute(LAST_LOGIN_TIME_ATTR);

        if (lastLatStr != null && lastLonStr != null && lastTimeStr != null) {
            try {
                return new LastLoginInfo(
                        new SimpleLocation(Double.parseDouble(lastLatStr), Double.parseDouble(lastLonStr)),
                        Long.parseLong(lastTimeStr)
                );
            } catch (NumberFormatException e) {
                logger.warnf("Invalid stored location data: %s", e.getMessage());
            }
        }

        return null;
    }

    private void saveLoginLocation(UserModel user, LocationData location) {
        if (location.getLatitude() != null && location.getLongitude() != null) {
            user.setSingleAttribute(LAST_LOGIN_LOCATION_ATTR + ".lat", location.getLatitude().toString());
            user.setSingleAttribute(LAST_LOGIN_LOCATION_ATTR + ".lon", location.getLongitude().toString());
            user.setSingleAttribute(LAST_LOGIN_TIME_ATTR, String.valueOf(Time.currentTimeMillis()));
        }
    }

    /**
     * Calculate distance between two points using the Haversine formula
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private static class LastLoginInfo {
        final SimpleLocation location;
        final long time;

        LastLoginInfo(SimpleLocation location, long time) {
            this.location = location;
            this.time = time;
        }
    }

    private static class SimpleLocation implements LocationData {
        private final Double latitude;
        private final Double longitude;

        SimpleLocation(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String getCity() { return null; }

        @Override
        public String getRegion() { return null; }

        @Override
        public String getRegionCode() { return null; }

        @Override
        public String getCountry() { return null; }

        @Override
        public String getContinent() { return null; }

        @Override
        public String getPostalCode() { return null; }

        @Override
        public Double getLatitude() { return latitude; }

        @Override
        public Double getLongitude() { return longitude; }

        @Override
        public String getTimezone() { return null; }

        @Override
        public String getCurrency() { return null; }
    }
}
