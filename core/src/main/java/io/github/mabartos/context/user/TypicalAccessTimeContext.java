package io.github.mabartos.context.user;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.evaluator.login.CircularEwmaProfile;
import io.github.mabartos.spi.context.AbstractUserContext;
import io.github.mabartos.spi.engine.OnSuccessfulLoginCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.keycloak.common.util.Time;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Context for managing typical access time patterns using circular EWMA statistics.
 * This context is shared by multiple evaluators to avoid duplication of time pattern analysis.
 * <p>
 * Values are updated in the {@link io.github.mabartos.engine.LoginEventsEventListener}
 */
public class TypicalAccessTimeContext extends AbstractUserContext<TypicalAccessTimeData> implements OnSuccessfulLoginCallback {
    private static final Logger logger = Logger.getLogger(TypicalAccessTimeContext.class);

    public static final String PROVIDER_ID = "typical-access-time-context";

    // User attribute keys for storing profile state
    private static final String ATTR_MEAN_SIN = "adaptive-time-pattern-meanSin";
    private static final String ATTR_MEAN_COS = "adaptive-time-pattern-meanCos";

    // EWMA smoothing factor - lower values give more weight to history
    private static final double ALPHA = 0.15;

    // Minimum number of historical logins required before evaluating risk
    // 4 historical logins + 1 current login = 5 total login attempts
    public static final int MIN_LOGINS = 4;

    private final LoginEventsContext loginEvents;

    public TypicalAccessTimeContext(KeycloakSession session) {
        super(session);
        this.loginEvents = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public Optional<TypicalAccessTimeData> initData(@Nonnull RealmModel realm, @Nullable UserModel user) {
        if (user == null) {
            logger.warn("User is null");
            return Optional.empty();
        }

        if (loginEvents == null) {
            logger.warn("LoginEventsContext not available");
            return Optional.empty();
        }

        // Get historical login events
        var eventsOptional = loginEvents.getData(realm, user);
        if (eventsOptional.isEmpty()) {
            return Optional.empty();
        }

        List<Event> loginOnlyEvents = eventsOptional.get().stream()
                .filter(e -> e.getType() == EventType.LOGIN)
                .toList();

        int loginCount = loginOnlyEvents.size();

        // Load or bootstrap the profile from HISTORICAL data only
        // The current login is NOT included yet (will be persisted after successful auth)
        CircularEwmaProfile profile = loadOrBootstrapProfile(realm, user, loginOnlyEvents);

        return Optional.of(new TypicalAccessTimeData(profile, loginCount));
    }

    /**
     * Updates the profile with current login hour and saves to user attributes after successful login
     */
    @Override
    public void onSuccessfulLogin(@Nonnull RealmModel realm, @Nonnull UserModel user) {
        int currentHour = Instant.ofEpochMilli(Time.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .getHour();

        CircularEwmaProfile profile = loadProfileFromAttributes(realm, user)
                .orElseGet(() -> new CircularEwmaProfile(ALPHA));

        profile.update(currentHour);
        saveProfile(realm, user, profile);
    }

    /**
     * Loads profile from user attributes or bootstraps from historical events if not found.
     */
    private CircularEwmaProfile loadOrBootstrapProfile(RealmModel realm, UserModel user, List<Event> loginEvents) {
        return loadProfileFromAttributes(realm, user).orElseGet(() -> {
            // No valid profile exists - bootstrap from historical login events
            logger.tracef("Bootstrapping time pattern for user %s from %d historical logins",
                    user.getUsername(), loginEvents.size());

            CircularEwmaProfile profile = new CircularEwmaProfile(ALPHA);
            for (Event event : loginEvents) {
                int hour = getHourFromEvent(event);
                profile.update(hour);
            }

            // Save bootstrapped profile
            saveProfile(realm, user, profile);
            return profile;
        });
    }

    /**
     * Loads profile from the same storage Keycloak uses for the user type.
     */
    private Optional<CircularEwmaProfile> loadProfileFromAttributes(RealmModel realm, UserModel user) {
        if (user.isFederated()) {
            var federatedAttributes = userFederatedStorage().getAttributes(realm, user.getId());
            return parseProfile(federatedAttributes.get(ATTR_MEAN_SIN), federatedAttributes.get(ATTR_MEAN_COS), user);
        }

        return parseProfile(user.getAttributes().get(ATTR_MEAN_SIN), user.getAttributes().get(ATTR_MEAN_COS), user);
    }

    private Optional<CircularEwmaProfile> parseProfile(List<String> meanSinValues, List<String> meanCosValues, UserModel user) {
        if (CollectionUtil.isEmpty(meanSinValues) || CollectionUtil.isEmpty(meanCosValues)) {
            return Optional.empty();
        }

        try {
            double meanSin = Double.parseDouble(meanSinValues.getFirst());
            double meanCos = Double.parseDouble(meanCosValues.getFirst());
            return Optional.of(new CircularEwmaProfile(ALPHA, meanSin, meanCos));
        } catch (NumberFormatException e) {
            logger.warnf("Failed to parse time pattern for user %s", user.getUsername());
            return Optional.empty();
        }
    }

    private void saveProfile(RealmModel realm, UserModel user, CircularEwmaProfile profile) {
        if (session.getTransactionManager().isActive()) {
            session.getTransactionManager().enlistPrepare(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    setAttributes(realm, user, profile);
                }

                @Override
                protected void rollbackImpl() {
                    // noop
                }
            });
        } else {
            setAttributes(realm, user, profile);
        }

        logger.tracef("Saved time pattern for user %s: meanSin=%.4f, meanCos=%.4f",
                user.getUsername(), profile.getMeanSin(), profile.getMeanCos());
    }

    private UserFederatedStorageProvider userFederatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }

    private int getHourFromEvent(Event event) {
        return Instant.ofEpochMilli(event.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .getHour();
    }

    private void setAttributes(RealmModel realm, UserModel user, CircularEwmaProfile profile) {
        if (user.isFederated()) {
            UserFederatedStorageProvider federatedStorage = userFederatedStorage();
            federatedStorage.setSingleAttribute(realm, user.getId(), ATTR_MEAN_SIN, String.valueOf(profile.getMeanSin()));
            federatedStorage.setSingleAttribute(realm, user.getId(), ATTR_MEAN_COS, String.valueOf(profile.getMeanCos()));
            return;
        }
        user.setSingleAttribute(ATTR_MEAN_SIN, String.valueOf(profile.getMeanSin()));
        user.setSingleAttribute(ATTR_MEAN_COS, String.valueOf(profile.getMeanCos()));
    }
}
