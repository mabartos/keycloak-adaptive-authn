package io.github.mabartos.context.location.maxmind;

import com.maxmind.geoip2.model.CityResponse;
import io.github.mabartos.context.location.LocationData;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Maps a MaxMind {@link CityResponse} to {@link LocationData}.
 */
final class MaxMindLocationData implements LocationData {

    private final String city;
    private final String region;
    private final String regionCode;
    private final String country;
    private final String continent;
    private final String postalCode;
    private final Double latitude;
    private final Double longitude;
    private final String timezone;

    private MaxMindLocationData(CityResponse response) {
        this.city = nullToNull(response.city() != null ? response.city().name() : null);
        this.region = nullToNull(response.mostSpecificSubdivision() != null
                ? response.mostSpecificSubdivision().name()
                : null);
        this.regionCode = nullToNull(response.mostSpecificSubdivision() != null
                ? response.mostSpecificSubdivision().isoCode()
                : null);
        this.country = nullToNull(response.country() != null ? response.country().name() : null);
        this.continent = nullToNull(response.continent() != null ? response.continent().code() : null);
        this.postalCode = nullToNull(response.postal() != null ? response.postal().code() : null);
        if (response.location() != null) {
            this.latitude = response.location().latitude();
            this.longitude = response.location().longitude();
            this.timezone = nullToNull(response.location().timeZone());
        } else {
            this.latitude = null;
            this.longitude = null;
            this.timezone = null;
        }
    }

    static Optional<LocationData> from(CityResponse response) {
        if (response == null) {
            return Optional.empty();
        }
        String countryName = response.country() != null ? response.country().name() : null;
        if (!StringUtil.isNotBlank(countryName)) {
            return Optional.empty();
        }
        return Optional.of(new MaxMindLocationData(response));
    }

    private static String nullToNull(String value) {
        return value != null && !value.isBlank() ? value : null;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getRegionCode() {
        return regionCode;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getContinent() {
        return continent;
    }

    @Override
    public String getPostalCode() {
        return postalCode;
    }

    @Override
    public Double getLatitude() {
        return latitude;
    }

    @Override
    public Double getLongitude() {
        return longitude;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public String getCurrency() {
        return null;
    }
}
