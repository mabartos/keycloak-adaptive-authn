package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.spi.context.AbstractUserContext;
import io.github.mabartos.spi.engine.OnSuccessfulLoginCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KnownLocationContext extends AbstractUserContext<Set<LocationData>> implements OnSuccessfulLoginCallback {
    private static final Logger logger = Logger.getLogger(KnownLocationContext.class);
    public static final String KNOWN_LOCATIONS_ATTR = "adaptive-location-known";
    public static final String TTL_DAYS_CONFIG = KnownLocationSettings.TTL_DAYS_CONFIG;
    public static final int DEFAULT_TTL_DAYS = KnownLocationSettings.DEFAULT_TTL_DAYS;

    public static int getTtlDays(RealmModel realm) {
        return KnownLocationSettings.ttlDays(realm);
    }

    public KnownLocationContext(KeycloakSession session) {
        super(session);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public Optional<Set<LocationData>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            logger.warn("User is null");
            return Optional.empty();
        }

        long now = Time.currentTime();
        int ttlDays = getTtlDays(realm);
        var activeLocations = selectActiveLocations(getKnownLocationData(knownUser), now, ttlDays);
        if (activeLocations.isEmpty()) {
            logger.trace("No known locations yet");
            return Optional.empty();
        }

        return Optional.of(Set.copyOf(activeLocations));
    }

    @Override
    public void onSuccessfulLogin(@Nonnull RealmModel realm, @Nonnull UserModel user) {
        LocationContext locationContext = UserContexts.getContext(session, LocationContext.class);
        LocationData location = locationContext.getData(realm, user).orElse(null);
        if (location == null) {
            logger.tracef("No current location");
            return;
        }

        long now = Time.currentTime();
        int ttlDays = getTtlDays(realm);
        var knownLocations = selectActiveLocations(getKnownLocationData(user), now, ttlDays);

        var currentKnownLocation = KnownLocationData.of(
                location.getCountry(),
                location.getCity(),
                now
        );

        // Remove if already present to update position (move to end)
        removeMatchingLocation(knownLocations, currentKnownLocation);
        knownLocations.add(currentKnownLocation);

        // Keep only the last N locations (realm setting)
        int maxStoredLocations = KnownLocationSettings.maxStoredLocations(realm);
        if (knownLocations.size() > maxStoredLocations) {
            knownLocations = knownLocations.stream()
                    .skip(knownLocations.size() - maxStoredLocations)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        saveKnownLocationData(user, knownLocations);
    }

    /**
     * Keeps non-expired locations and backfills legacy entries missing a timestamp.
     * In-memory during {@link #initData}; persisted in {@link #onSuccessfulLogin}.
     */
    private static LinkedHashSet<KnownLocationData> selectActiveLocations(
            Set<KnownLocationData> rawLocations, long now, int ttlDays) {
        var activeLocations = new LinkedHashSet<KnownLocationData>();
        for (KnownLocationData location : rawLocations) {
            if (location.isExpired(now, ttlDays)) {
                continue;
            }
            activeLocations.add(location.ensureLastSeen(now));
        }
        return activeLocations;
    }

    private LinkedHashSet<KnownLocationData> getKnownLocationData(UserModel knownUser) {
        return knownUser.getAttributeStream(KNOWN_LOCATIONS_ATTR)
                .map(KnownLocationData::parseFromAttribute)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void saveKnownLocationData(UserModel user, Set<KnownLocationData> knownLocations) {
        var locationKeys = knownLocations.stream()
                .map(KnownLocationData::formatToAttribute)
                .toList();
        user.setAttribute(KNOWN_LOCATIONS_ATTR, locationKeys);
    }

    private void removeMatchingLocation(Set<KnownLocationData> locations, KnownLocationData toRemove) {
        locations.removeIf(loc -> loc.matches(toRemove.getCountry(), toRemove.getCity()));
    }
}
