package io.github.mabartos.context.device;

import org.jboss.logging.Logger;
import org.keycloak.utils.StringUtil;

import java.util.Objects;

/**
 * Known device stored in user attributes with a last-seen timestamp.
 * Attribute format: {@code visitorId:epochSeconds}. Legacy entries without a timestamp
 * are backfilled on first read.
 */
public record KnownDeviceData(String visitorId, Long lastSeenEpochSeconds) {

    private static final Logger logger = Logger.getLogger(KnownDeviceData.class);
    private static final String SEPARATOR = ":";
    private static final long SECONDS_PER_DAY = 86_400L;

    public static KnownDeviceData of(String visitorId, long lastSeenEpochSeconds) {
        return new KnownDeviceData(visitorId, lastSeenEpochSeconds);
    }

    public static KnownDeviceData parseFromAttribute(String attributeValue) {
        if (StringUtil.isBlank(attributeValue)) {
            return null;
        }

        int separatorIndex = attributeValue.indexOf(SEPARATOR);
        if (separatorIndex > 0) {
            String visitorId = attributeValue.substring(0, separatorIndex);
            String timestamp = attributeValue.substring(separatorIndex + 1);
            if (!VisitorIdUtils.isValidVisitorId(visitorId)) {
                return null;
            }
            Long lastSeen = null;
            if (StringUtil.isNotBlank(timestamp)) {
                try {
                    long parsed = Long.parseLong(timestamp);
                    if (parsed > 0) {
                        lastSeen = parsed;
                    }
                } catch (NumberFormatException e) {
                    logger.debugf("Invalid known device timestamp: %s", attributeValue);
                    return null;
                }
            }
            return new KnownDeviceData(visitorId, lastSeen);
        }

        if (VisitorIdUtils.isValidVisitorId(attributeValue)) {
            return new KnownDeviceData(attributeValue, null);
        }
        return null;
    }

    public String formatToAttribute() {
        if (lastSeenEpochSeconds == null || lastSeenEpochSeconds <= 0) {
            return visitorId;
        }
        return visitorId + SEPARATOR + lastSeenEpochSeconds;
    }

    public KnownDeviceData withLastSeen(long lastSeenEpochSeconds) {
        return new KnownDeviceData(visitorId, lastSeenEpochSeconds);
    }

    public boolean isUndated() {
        return lastSeenEpochSeconds == null || lastSeenEpochSeconds <= 0;
    }

    public KnownDeviceData ensureLastSeen(long now) {
        return isUndated() ? withLastSeen(now) : this;
    }

    public boolean matches(String otherVisitorId) {
        return Objects.equals(visitorId, otherVisitorId);
    }

    public boolean isExpired(long now, int ttlDays) {
        if (ttlDays <= 0 || lastSeenEpochSeconds == null || lastSeenEpochSeconds <= 0) {
            return false;
        }
        return now - lastSeenEpochSeconds > (long) ttlDays * SECONDS_PER_DAY;
    }
}
