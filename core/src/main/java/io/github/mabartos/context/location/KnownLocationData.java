package io.github.mabartos.context.location;

import org.jboss.logging.Logger;
import org.keycloak.utils.StringUtil;

import java.util.Objects;

/**
 * Known location stored in user attributes with a last-seen timestamp.
 * Attribute format: {@code country:city:epochSeconds}. Legacy entries without a timestamp
 * are backfilled on first read.
 */
public record KnownLocationData(String country, String city, Integer lastSeenEpochSeconds) implements LocationData {

    private static final Logger logger = Logger.getLogger(KnownLocationData.class);
    private static final String SEPARATOR = ":";
    private static final int SECONDS_PER_DAY = 86_400;

    public static KnownLocationData of(String country, String city, int lastSeenEpochSeconds) {
        return new KnownLocationData(country, city, lastSeenEpochSeconds);
    }

    public static KnownLocationData parseFromAttribute(String attributeValue) {
        if (StringUtil.isBlank(attributeValue)) {
            return null;
        }

        var parts = attributeValue.split(SEPARATOR, 3);
        if (parts.length < 2) {
            return null;
        }

        Integer lastSeen = null;
        if (parts.length == 3 && StringUtil.isNotBlank(parts[2])) {
            try {
                int parsed = Integer.parseInt(parts[2]);
                if (parsed > 0) {
                    lastSeen = parsed;
                }
            } catch (NumberFormatException e) {
                logger.debugf("Invalid known location timestamp: %s", attributeValue);
                return null;
            }
        }

        return new KnownLocationData(parts[0], parts[1], lastSeen);
    }

    public String formatToAttribute() {
        if (lastSeenEpochSeconds == null || lastSeenEpochSeconds <= 0) {
            return country + SEPARATOR + city;
        }
        return country + SEPARATOR + city + SEPARATOR + lastSeenEpochSeconds;
    }

    public KnownLocationData withLastSeen(int lastSeenEpochSeconds) {
        return new KnownLocationData(country, city, lastSeenEpochSeconds);
    }

    public boolean isUndated() {
        return lastSeenEpochSeconds == null || lastSeenEpochSeconds <= 0;
    }

    public KnownLocationData ensureLastSeen(int now) {
        return isUndated() ? withLastSeen(now) : this;
    }

    public boolean matches(String otherCountry, String otherCity) {
        return Objects.equals(country, otherCountry) && Objects.equals(city, otherCity);
    }

    public boolean isExpired(int now, int ttlDays) {
        if (ttlDays <= 0 || lastSeenEpochSeconds == null || lastSeenEpochSeconds <= 0) {
            return false;
        }
        return now - lastSeenEpochSeconds > ttlDays * SECONDS_PER_DAY;
    }

    @Override
    public String getCity() {
        return StringUtil.isNotBlank(city) ? city : null;
    }

    @Override
    public String getRegion() {
        return null;
    }

    @Override
    public String getRegionCode() {
        return null;
    }

    @Override
    public String getCountry() {
        return StringUtil.isNotBlank(country) ? country : null;
    }

    @Override
    public String getContinent() {
        return null;
    }

    @Override
    public String getPostalCode() {
        return null;
    }

    @Override
    public Double getLatitude() {
        return null;
    }

    @Override
    public Double getLongitude() {
        return null;
    }

    @Override
    public String getTimezone() {
        return null;
    }

    @Override
    public String getCurrency() {
        return null;
    }
}
