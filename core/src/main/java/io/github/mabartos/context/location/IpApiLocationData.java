/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.context.location;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * Response data from the 'ipapi.co' server
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IpApiLocationData implements LocationData, Serializable {
    private String city;
    private String region;
    private String region_code;
    private String country_name;
    private String country_capital;
    private String continent_code;
    private String postal;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private String currency;

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
        return region_code;
    }

    @Override
    public String getCountry() {
        return country_name;
    }

    @Override
    public String getContinent() {
        return continent_code;
    }

    @Override
    public String getPostalCode() {
        return postal;
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
        return currency;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", IpApiLocationData.class.getSimpleName() + " {", "}")
                .add("city='" + city + "'")
                .add("region='" + region + "'")
                .add("region_code='" + region_code + "'")
                .add("country_name='" + country_name + "'")
                .add("country_capital='" + country_capital + "'")
                .add("continent_code='" + continent_code + "'")
                .add("postal='" + postal + "'")
                .add("latitude=" + latitude)
                .add("longitude=" + longitude)
                .add("timezone='" + timezone + "'")
                .add("currency='" + currency + "'")
                .toString();
    }

}
