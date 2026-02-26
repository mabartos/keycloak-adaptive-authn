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

import java.util.List;
import java.util.Set;

import static io.github.mabartos.level.Risk.Score.INTERMEDIATE;
import static io.github.mabartos.level.Risk.Score.NONE;
import static io.github.mabartos.level.Risk.Score.SMALL;

/**
 * Risk evaluator for location properties
 */
public class KnownLocationRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(KnownLocationRiskEvaluator.class);
    private static final String KNOWN_LOCATIONS_ATTR = "adaptive_authn.known_locations";
    // TODO later, it might be configurable
    private static final int MAX_STORED_LOCATIONS = 10;

    private final IpApiLocationContext locationContext;

    public KnownLocationRiskEvaluator(KeycloakSession session) {
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
            return Risk.invalid("Cannot obtain location information");
        }

        logger.tracef("Current location: %s", currentLocation.toString());

        // Get known locations for this user
        List<String> knownLocations = getKnownLocations(knownUser);

        if (knownLocations.isEmpty()) {
            return Risk.of(SMALL, "First tracked location");
        }

        saveSuccessfulLoginLocation(knownUser, knownLocations, currentLocation);

        return calculateLocationRisk(currentLocation, knownLocations);
    }

    protected Risk calculateLocationRisk(LocationData currentLocation, List<String> knownLocations) {
        String currentLocationKey = getLocationKey(currentLocation);

        boolean exactMatch = knownLocations.contains(currentLocationKey);
        if (exactMatch) {
            return Risk.of(NONE, "This exact location (city + country) has been seen before");
        }

        boolean sameCountry = knownLocations.stream()
                .anyMatch(loc -> loc.endsWith(":" + currentLocation.getCountry()));
        if (sameCountry) {
            return Risk.of(SMALL, "The city has changed, but the country is the same.");
        }

        return Risk.of(INTERMEDIATE, "Completely new country");
    }

    protected List<String> getKnownLocations(UserModel user) {
        return user.getAttributeStream(KNOWN_LOCATIONS_ATTR).toList();
    }

    public void saveSuccessfulLoginLocation(UserModel user, List<String> knownLocations, LocationData currentLocation) {
        if (currentLocation == null) {
            return;
        }

        String locationKey = getLocationKey(currentLocation);

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
            user.setAttribute(KNOWN_LOCATIONS_ATTR, knownLocations);
            logger.tracef("Saved location for user %s: %s", user.getUsername(), locationKey);
        }
    }

    protected String getLocationKey(LocationData location) {
        return location.getCity() + ":" + location.getCountry();
    }
}
