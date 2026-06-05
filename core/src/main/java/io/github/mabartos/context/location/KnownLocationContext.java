package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.evaluator.login.CircularEwmaProfile;
import io.github.mabartos.spi.context.AbstractUserContext;
import io.github.mabartos.spi.engine.OnSuccessfulLoginCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KnownLocationContext extends AbstractUserContext<Set<LocationData>> implements OnSuccessfulLoginCallback {
    private static final Logger logger = Logger.getLogger(KnownLocationContext.class);
    private static final String KNOWN_LOCATIONS_ATTR = "adaptive-location-known";
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
    public Optional<Set<LocationData>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            logger.warn("User is null");
            return Optional.empty();
        }

        var knownLocations = getKnownLocationData(realm, knownUser);

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

        var currentKnownLocation = LocationDataUtils.create(location.getCountry(), location.getCity());
        var knownLocations = getKnownLocationData(realm, user);

        // Remove if already present to update position (move to end)
        removeMatchingLocation(knownLocations, currentKnownLocation);
        knownLocations.add(currentKnownLocation);

        // Keep only the last N locations
        if (knownLocations.size() > MAX_STORED_LOCATIONS) {
            knownLocations = knownLocations.stream()
                    .skip(knownLocations.size() - MAX_STORED_LOCATIONS)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        // Save back to user attributes
        saveKnownLocationData(realm, user, knownLocations);
    }

    private LinkedHashSet<LocationData> getKnownLocationData(RealmModel realm, UserModel knownUser) {
        if (knownUser.isFederated()) {
            return userFederatedStorage().getAttributes(realm, knownUser.getId())
                    .getOrDefault(KNOWN_LOCATIONS_ATTR, List.of()).stream()
                    .map(LocationDataUtils::parseFromAttribute)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return knownUser.getAttributeStream(KNOWN_LOCATIONS_ATTR)
                .map(LocationDataUtils::parseFromAttribute)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void saveKnownLocationData(RealmModel realm, UserModel user, Set<LocationData> knownLocations) {
        var locationKeys = knownLocations.stream()
                .map(LocationDataUtils::formatToAttribute)
                .toList();
        if (session.getTransactionManager().isActive()) {
            session.getTransactionManager().enlistPrepare(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    setAttributes(realm, user, locationKeys);
                }

                @Override
                protected void rollbackImpl() {
                    // noop
                }
            });
            return;
        }

        setAttributes(realm, user, locationKeys);
    }

    private void removeMatchingLocation(Set<LocationData> locations, LocationData toRemove) {
        locations.removeIf(loc ->
                Objects.equals(loc.getCountry(), toRemove.getCountry()) &&
                Objects.equals(loc.getCity(), toRemove.getCity())
        );
    }

    private UserFederatedStorageProvider userFederatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }

    private void setAttributes(RealmModel realm, UserModel user, List<String> locationKeys) {
        if (user.isFederated()) {
            userFederatedStorage().setAttribute(realm, user.getId(), KNOWN_LOCATIONS_ATTR, locationKeys);
            return;
        }
        user.setAttribute(KNOWN_LOCATIONS_ATTR, locationKeys);
    }
}
