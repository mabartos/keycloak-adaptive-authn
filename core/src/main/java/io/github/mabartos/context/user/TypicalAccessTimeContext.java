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
    private static final String ATTR_MEAN_SIN = "timePattern.meanSin";
    private static final String ATTR_MEAN_COS = "timePattern.meanCos";

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
        CircularEwmaProfile profile = loadOrBootstrapProfile(user, loginOnlyEvents);

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

        CircularEwmaProfile profile = loadProfileFromAttributes(user)
                .orElseGet(() -> new CircularEwmaProfile(ALPHA));

        profile.update(currentHour);
        saveProfile(user, profile);
    }

    /**
     * Loads profile from user attributes or bootstraps from historical events if not found.
     */
    private CircularEwmaProfile loadOrBootstrapProfile(UserModel user, List<Event> loginEvents) {
        return loadProfileFromAttributes(user).orElseGet(() -> {
            // No valid profile exists - bootstrap from historical login events
            logger.tracef("Bootstrapping time pattern for user %s from %d historical logins",
                    user.getUsername(), loginEvents.size());

            CircularEwmaProfile profile = new CircularEwmaProfile(ALPHA);
            for (Event event : loginEvents) {
                int hour = getHourFromEvent(event);
                profile.update(hour);
            }

            // Save bootstrapped profile
            saveProfile(user, profile);
            return profile;
        });
    }

    /**
     * Loads profile from user attributes, returns empty if not found or invalid.
     */
    private Optional<CircularEwmaProfile> loadProfileFromAttributes(UserModel user) {
        List<String> meanSinValues = user.getAttributes().get(ATTR_MEAN_SIN);
        List<String> meanCosValues = user.getAttributes().get(ATTR_MEAN_COS);

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

    private void saveProfile(UserModel user, CircularEwmaProfile profile) {
        user.setSingleAttribute(ATTR_MEAN_SIN, String.valueOf(profile.getMeanSin()));
        user.setSingleAttribute(ATTR_MEAN_COS, String.valueOf(profile.getMeanCos()));

        logger.tracef("Saved time pattern for user %s: meanSin=%.4f, meanCos=%.4f",
                user.getUsername(), profile.getMeanSin(), profile.getMeanCos());
    }

    private int getHourFromEvent(Event event) {
        return Instant.ofEpochMilli(event.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .getHour();
    }
}
