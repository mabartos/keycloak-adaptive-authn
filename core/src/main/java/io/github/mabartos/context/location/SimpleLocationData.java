package io.github.mabartos.context.location;

import org.keycloak.utils.StringUtil;

/**
 * Simple LocationData implementation containing only country and city
 */
public class SimpleLocationData implements LocationData {
    private final String country;
    private final String city;

    public SimpleLocationData(String country, String city) {
        this.country = country;
        this.city = city;
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
