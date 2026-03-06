package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.spi.context.AbstractUserContext;
import io.github.mabartos.spi.engine.OnSuccessfulLoginCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KnownLocationContext extends AbstractUserContext<Set<KnownLocationContext.KnownLocationData>> implements OnSuccessfulLoginCallback {
    private static final Logger logger = Logger.getLogger(KnownLocationContext.class);
    private static final String KNOWN_LOCATIONS_ATTR = "adaptive_authn.known_locations";
    // TODO later, it might be configurable
    private static final int MAX_STORED_LOCATIONS = 10;

    public KnownLocationContext(KeycloakSession session) {
        super(session);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public Optional<Set<KnownLocationData>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            logger.warn("User is null");
            return Optional.empty();
        }

        var knownLocations = getKnownLocationData(knownUser);

        if (knownLocations.isEmpty()) {
            logger.trace("No known locations yet");
            return Optional.empty();
        }

        return Optional.of(knownLocations);
    }

    @Override
    public void onSuccessfulLogin(@Nonnull RealmModel realm, @Nonnull UserModel user) {
        LocationContext locationContext = UserContexts.getContext(session, LocationContext.class);
        LocationData location = locationContext.getData(realm, user).orElse(null);
        if (location == null) {
            logger.tracef("No current location");
            return;
        }

        var currentKnownLocation = getKnownLocationData(location.getCountry(), location.getCity());
        var knownLocations = getKnownLocationData(user);

        // Remove if already present to update position (move to end)
        knownLocations.remove(currentKnownLocation);
        knownLocations.add(currentKnownLocation);

        // Keep only the last N locations
        if (knownLocations.size() > MAX_STORED_LOCATIONS) {
            knownLocations = knownLocations.stream()
                    .skip(knownLocations.size() - MAX_STORED_LOCATIONS)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        // Save back to user attributes
        saveKnownLocationData(user, knownLocations);
    }

    public record KnownLocationData(String country, String city) {
    }

    private LinkedHashSet<KnownLocationData> getKnownLocationData(UserModel knownUser) {
        return knownUser.getAttributeStream(KNOWN_LOCATIONS_ATTR)
                .map(this::parseAttribute)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void saveKnownLocationData(UserModel user, Set<KnownLocationData> knownLocations) {
        var locationKeys = knownLocations.stream()
                .map(this::getKnownLocationKey)
                .toList();
        user.setAttribute(KNOWN_LOCATIONS_ATTR, locationKeys);
    }

    private KnownLocationData parseAttribute(String attribute) {
        return Optional.ofNullable(attribute)
                .map(attr -> {
                    var parts = attr.split(":");
                    return getKnownLocationData(parts[0], parts[1]);
                }).orElse(null);
    }

    private String getKnownLocationKey(KnownLocationData location) {
        return location.country + ":" + location.city;
    }

    private KnownLocationData getKnownLocationData(String country, String city) {
        return new KnownLocationData(country, city);
    }


}
