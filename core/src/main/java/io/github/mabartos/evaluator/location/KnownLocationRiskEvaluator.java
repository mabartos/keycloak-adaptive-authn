package io.github.mabartos.evaluator.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.location.KnownLocationContext;
import io.github.mabartos.context.location.LocationContext;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Objects;
import java.util.Set;

/**
 * Risk evaluator for location properties
 */
@EvaluationPhase(USER_KNOWN)
public class KnownLocationRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(KnownLocationRiskEvaluator.class);

    private final LocationContext locationContext;
    private final KnownLocationContext knownLocationContext;

    public KnownLocationRiskEvaluator(KeycloakSession session) {
        this.locationContext = UserContexts.getContext(session, LocationContext.class);
        this.knownLocationContext = UserContexts.getContext(session, KnownLocationContext.class);
    }

    KnownLocationRiskEvaluator(LocationContext locationContext, KnownLocationContext knownLocationContext) {
        this.locationContext = locationContext;
        this.knownLocationContext = knownLocationContext;
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

        var knownLocations = knownLocationContext.getData(realm, knownUser).orElse(Set.of());

        if (knownLocations.isEmpty()) {
            return Risk.of(KnownLocationEvaluatorConfig.firstLocationScore(realm), "First tracked location");
        }

        return calculateLocationRisk(realm, currentLocation, knownLocations);
    }

    protected Risk calculateLocationRisk(
            RealmModel realm, LocationData currentLocation, Set<LocationData> knownLocations) {
        if (currentLocation.getCountry() == null) {
            return Risk.invalid("Cannot determine country from IP address");
        }

        boolean exactMatch = knownLocations.stream()
                .anyMatch(loc ->
                        Objects.equals(loc.getCountry(), currentLocation.getCountry()) &&
                        Objects.equals(loc.getCity(), currentLocation.getCity()));
        if (exactMatch) {
            return Risk.of(
                    KnownLocationEvaluatorConfig.knownLocationScore(realm),
                    "Known location (city + country) - trust signal");
        }

        boolean sameCountry = knownLocations.stream()
                .anyMatch(loc -> Objects.equals(loc.getCountry(), currentLocation.getCountry()));
        if (sameCountry) {
            return Risk.of(
                    KnownLocationEvaluatorConfig.sameCountryScore(realm),
                    "Same country, different city - minor anomaly");
        }

        return Risk.of(KnownLocationEvaluatorConfig.newCountryScore(realm), "Completely new country");
    }
}
