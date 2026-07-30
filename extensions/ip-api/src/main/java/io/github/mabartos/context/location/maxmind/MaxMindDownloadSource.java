package io.github.mabartos.context.location.maxmind;

/**
 * Download source for the GeoLite2-City database.
 */
public enum MaxMindDownloadSource {
    /** MaxMind official HTTPS API (requires account id and license key). */
    OFFICIAL,
    /** Community mirror for quick POC (not for production; see GeoLite2 EULA). */
    MIRROR
}
