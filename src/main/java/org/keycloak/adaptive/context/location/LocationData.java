package org.keycloak.adaptive.context.location;

public interface LocationData {
    String getCity();

    String getRegion();

    String getRegionCode();

    String getCountry();

    String getContinent();

    String getPostalCode();

    Double getLatitude();

    Double getLongitude();

    String getTimezone();

    String getCurrency();
}
