package io.github.mabartos.context.location;

import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Utility class for parsing and formatting LocationData
 */
public class LocationDataUtils {

    private static final String LOCATION_SEPARATOR = ":";

    /**
     * Parse location data from "country:city" format
     *
     * @param attributeValue string in "country:city" format
     * @return LocationData or null if invalid
     */
    public static LocationData parseFromAttribute(String attributeValue) {
        return Optional.ofNullable(attributeValue)
                .filter(StringUtil::isNotBlank)
                .map(attr -> {
                    var parts = attr.split(LOCATION_SEPARATOR, 2);
                    if (parts.length == 2) {
                        return new SimpleLocationData(parts[0], parts[1]);
                    }
                    return null;
                }).orElse(null);
    }

    /**
     * Format location data to "country:city" format
     *
     * @param location LocationData to format
     * @return string in "country:city" format
     */
    public static String formatToAttribute(LocationData location) {
        if (location == null) {
            return "";
        }
        String country = Optional.ofNullable(location.getCountry()).orElse("");
        String city = Optional.ofNullable(location.getCity()).orElse("");
        return country + LOCATION_SEPARATOR + city;
    }

    /**
     * Create a simple LocationData with country and city
     *
     * @param country country name
     * @param city    city name
     * @return LocationData instance
     */
    public static LocationData create(String country, String city) {
        return new SimpleLocationData(country, city);
    }
}
