package io.github.mabartos.context.location;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * Response payload for <a href="https://ip-api.com">ip-api.com</a> pro JSON API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IpApiComLocationData implements LocationData, Serializable {

    private String status;
    private String message;
    private String country;
    private String region;
    private String regionName;
    private String city;
    private String zip;
    private Double lat;
    private Double lon;
    private String timezone;
    @JsonProperty("as")
    private String as;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public String getStatusMessage() {
        return message;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getRegion() {
        return regionName;
    }

    @Override
    public String getRegionCode() {
        return region;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getContinent() {
        return null;
    }

    @Override
    public String getPostalCode() {
        return zip;
    }

    @Override
    public Double getLatitude() {
        return lat;
    }

    @Override
    public Double getLongitude() {
        return lon;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public String getCurrency() {
        return null;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", IpApiComLocationData.class.getSimpleName() + " {", "}")
                .add("status='" + status + "'")
                .add("country='" + country + "'")
                .add("regionName='" + regionName + "'")
                .add("city='" + city + "'")
                .add("zip='" + zip + "'")
                .add("lat=" + lat)
                .add("lon=" + lon)
                .add("timezone='" + timezone + "'")
                .add("as='" + as + "'")
                .toString();
    }
}
