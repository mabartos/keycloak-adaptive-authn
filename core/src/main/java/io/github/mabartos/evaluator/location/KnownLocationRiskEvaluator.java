package io.github.mabartos.evaluator.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.location.IpApiLocationContext;
import io.github.mabartos.context.location.IpApiLocationContextFactory;
import io.github.mabartos.context.location.KnownLocationContext;
import io.github.mabartos.context.location.KnownLocationContextFactory;
import io.github.mabartos.context.location.LocationContext;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

import static io.github.mabartos.level.Risk.Score.HIGH;
import static io.github.mabartos.level.Risk.Score.NONE;
import static io.github.mabartos.level.Risk.Score.SMALL;

/**
 * Risk evaluator for location properties
 */
public class KnownLocationRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(KnownLocationRiskEvaluator.class);

    private final LocationContext locationContext;
    private final KnownLocationContext knownLocationContext;

    public KnownLocationRiskEvaluator(KeycloakSession session) {
        this.locationContext = UserContexts.getContext(session, LocationContext.class);
        this.knownLocationContext = UserContexts.getContext(session, KnownLocationContext.class);
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

        // Get known locations for this user from KnownLocationContext
        var knownLocations = knownLocationContext.getData(realm, knownUser).orElse(Set.of());

        if (knownLocations.isEmpty()) {
            return Risk.of(SMALL, "First tracked location");
        }

        return calculateLocationRisk(currentLocation, knownLocations);
    }

    protected Risk calculateLocationRisk(LocationData currentLocation, Set<KnownLocationContext.KnownLocationData> knownLocations) {
        var currentKnownLocation = new KnownLocationContext.KnownLocationData(
                currentLocation.getCountry(),
                currentLocation.getCity()
        );

        // Check for exact match (same city and country)
        boolean exactMatch = knownLocations.contains(currentKnownLocation);
        if (exactMatch) {
            return Risk.of(NONE, "This exact location (city + country) has been seen before");
        }

        // Check if the country has been seen before (even if city is different)
        boolean sameCountry = knownLocations.stream()
                .anyMatch(loc -> loc.country().equals(currentLocation.getCountry()));
        if (sameCountry) {
            return Risk.of(SMALL, "The city has changed, but the country is the same.");
        }

        return Risk.of(HIGH, "Completely new country");
    }
}
