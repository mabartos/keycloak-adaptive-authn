package io.github.mabartos.context.location;

/**
 * Sentinel {@link LocationData} when every GeoIP resolver failed: every string field is {@value #UNKNOWN}
 * so authentication flows can match on {@code Unknown} for country, city, etc. Coordinates stay {@code null}
 * (no meaningful numeric sentinel).
 */
public final class UnknownLocationData implements LocationData {
    public static final String UNKNOWN = "Unknown";
    public static final UnknownLocationData INSTANCE = new UnknownLocationData();

    private UnknownLocationData() {
    }

    @Override
    public String getCity() {
        return UNKNOWN;
    }

    @Override
    public String getRegion() {
        return UNKNOWN;
    }

    @Override
    public String getRegionCode() {
        return UNKNOWN;
    }

    @Override
    public String getCountry() {
        return UNKNOWN;
    }

    @Override
    public String getContinent() {
        return UNKNOWN;
    }

    @Override
    public String getPostalCode() {
        return UNKNOWN;
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
        return UNKNOWN;
    }

    @Override
    public String getCurrency() {
        return UNKNOWN;
    }
}
