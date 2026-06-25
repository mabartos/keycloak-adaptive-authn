package io.github.mabartos.context.device;

import io.github.mabartos.spi.context.AbstractUserContext;
import io.github.mabartos.spi.engine.OnSuccessfulLoginCallback;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KnownDeviceContext extends AbstractUserContext<Set<KnownDeviceData>> implements OnSuccessfulLoginCallback {
    private static final Logger logger = Logger.getLogger(KnownDeviceContext.class);
    public static final String KNOWN_DEVICES_ATTR = "adaptive-device-known";
    public static final String TTL_DAYS_SETTING_KEY = "ttl-days";
    public static final String TTL_DAYS_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            "KnownDeviceRiskEvaluator", TTL_DAYS_SETTING_KEY);
    public static final int DEFAULT_TTL_DAYS = 90;
    public static final String MAX_STORED_DEVICES_SETTING_KEY = "max-stored-devices";
    public static final String MAX_STORED_DEVICES_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            "KnownDeviceRiskEvaluator", MAX_STORED_DEVICES_SETTING_KEY);
    public static final int DEFAULT_MAX_STORED_DEVICES = 10;

    /**
     * Visitor ID captured from the auth note during {@link #initData} (USER_KNOWN phase).
     * Survives until {@link #onSuccessfulLogin} when the authentication session may already be gone.
     */
    private String pendingVisitorId;

    public static int getTtlDays(RealmModel realm) {
        if (realm == null) {
            return DEFAULT_TTL_DAYS;
        }
        var value = realm.getAttribute(TTL_DAYS_CONFIG);
        if (StringUtil.isBlank(value)) {
            return DEFAULT_TTL_DAYS;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warnf("Invalid known device TTL realm attribute '%s', using default %d", value, DEFAULT_TTL_DAYS);
            return DEFAULT_TTL_DAYS;
        }
    }

    public static int getMaxStoredDevices(RealmModel realm) {
        if (realm == null) {
            return DEFAULT_MAX_STORED_DEVICES;
        }
        var value = realm.getAttribute(MAX_STORED_DEVICES_CONFIG);
        if (StringUtil.isBlank(value)) {
            return DEFAULT_MAX_STORED_DEVICES;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                logger.warnf(
                        "Invalid known device max-stored-devices realm attribute '%s', using default %d",
                        value,
                        DEFAULT_MAX_STORED_DEVICES);
                return DEFAULT_MAX_STORED_DEVICES;
            }
            return parsed;
        } catch (NumberFormatException e) {
            logger.warnf(
                    "Invalid known device max-stored-devices realm attribute '%s', using default %d",
                    value,
                    DEFAULT_MAX_STORED_DEVICES);
            return DEFAULT_MAX_STORED_DEVICES;
        }
    }

    public KnownDeviceContext(KeycloakSession session) {
        super(session);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public Optional<Set<KnownDeviceData>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        capturePendingVisitorIdFromAuthSession();

        if (knownUser == null) {
            logger.warn("User is null");
            return Optional.empty();
        }

        long now = Time.currentTime();
        int ttlDays = getTtlDays(realm);
        var activeDevices = selectActiveDevices(getKnownDeviceData(knownUser), now, ttlDays);
        if (activeDevices.isEmpty()) {
            logger.trace("No known devices yet");
            return Optional.empty();
        }

        return Optional.of(Set.copyOf(activeDevices));
    }

    @Override
    public void onSuccessfulLogin(@Nonnull RealmModel realm, @Nonnull UserModel user) {
        resolveCurrentVisitorId().ifPresent(visitorId -> registerKnownDevice(realm, user, visitorId));
    }

    private void capturePendingVisitorIdFromAuthSession() {
        var authSession = session.getContext().getAuthenticationSession();
        if (authSession == null) {
            return;
        }
        var fromAuthNote = authSession.getAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE);
        if (VisitorIdUtils.isValidVisitorId(fromAuthNote)) {
            pendingVisitorId = fromAuthNote;
            logger.trace("Captured device visitor id during USER_KNOWN init");
        }
    }

    private Optional<String> resolveCurrentVisitorId() {
        if (VisitorIdUtils.isValidVisitorId(pendingVisitorId)) {
            return Optional.of(pendingVisitorId);
        }

        var authSession = session.getContext().getAuthenticationSession();
        if (authSession != null) {
            var fromAuthNote = authSession.getAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE);
            if (VisitorIdUtils.isValidVisitorId(fromAuthNote)) {
                return Optional.of(fromAuthNote);
            }
        }

        logger.trace("No device visitor id available on successful login");
        return Optional.empty();
    }

    private void registerKnownDevice(RealmModel realm, UserModel user, String visitorId) {
        long now = Time.currentTime();
        int ttlDays = getTtlDays(realm);
        int maxStoredDevices = getMaxStoredDevices(realm);
        var knownDevices = selectActiveDevices(getKnownDeviceData(user), now, ttlDays);

        var currentDevice = KnownDeviceData.of(visitorId, now);

        // Remove if already present to update position (move to end)
        removeMatchingDevice(knownDevices, currentDevice);
        knownDevices.add(currentDevice);

        // Keep only the last N devices
        if (knownDevices.size() > maxStoredDevices) {
            knownDevices = knownDevices.stream()
                    .skip(knownDevices.size() - maxStoredDevices)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        saveKnownDeviceData(user, knownDevices);
        logger.debugf(
                "Registered known device for user %s (visitorId=%s, lastSeen=%s)",
                user.getUsername(),
                visitorId,
                now
        );
    }

    /**
     * Keeps non-expired devices and backfills legacy entries missing a timestamp.
     * In-memory during {@link #initData}; persisted in {@link #onSuccessfulLogin}.
     */
    private static LinkedHashSet<KnownDeviceData> selectActiveDevices(
            Set<KnownDeviceData> rawDevices, long now, int ttlDays) {
        var activeDevices = new LinkedHashSet<KnownDeviceData>();
        for (KnownDeviceData device : rawDevices) {
            if (device.isExpired(now, ttlDays)) {
                continue;
            }
            activeDevices.add(device.ensureLastSeen(now));
        }
        return activeDevices;
    }

    private LinkedHashSet<KnownDeviceData> getKnownDeviceData(UserModel user) {
        return user.getAttributeStream(KNOWN_DEVICES_ATTR)
                .map(KnownDeviceData::parseFromAttribute)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void saveKnownDeviceData(UserModel user, Set<KnownDeviceData> knownDevices) {
        var deviceKeys = knownDevices.stream()
                .map(KnownDeviceData::formatToAttribute)
                .toList();
        user.setAttribute(KNOWN_DEVICES_ATTR, deviceKeys);
    }

    private void removeMatchingDevice(Set<KnownDeviceData> devices, KnownDeviceData toRemove) {
        devices.removeIf(device -> device.matches(toRemove.visitorId()));
    }
}
