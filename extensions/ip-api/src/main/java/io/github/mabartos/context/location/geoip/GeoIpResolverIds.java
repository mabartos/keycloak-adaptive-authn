package io.github.mabartos.context.location.geoip;

/**
 * Stable provider ids for HTTP GeoIP backends shipped with the ip-api extension.
 */
public final class GeoIpResolverIds {

    public static final String IPAPI_CO_FREE = "ipapi-co-free";
    public static final String IPAPI_CO_PRO = "ipapi-co-pro";
    public static final String IP_API_COM_FREE = "ip-api-com-free";
    public static final String IP_API_COM_PRO = "ip-api-com-pro";

    public static final String DEFAULT_FALLBACK = IPAPI_CO_FREE;

    private GeoIpResolverIds() {
    }
}
