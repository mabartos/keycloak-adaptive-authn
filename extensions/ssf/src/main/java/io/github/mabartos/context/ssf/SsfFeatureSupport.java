package io.github.mabartos.context.ssf;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;

public final class SsfFeatureSupport {

    private static final Logger logger = Logger.getLogger(SsfFeatureSupport.class);

    private SsfFeatureSupport() {
    }

    public static boolean isSsfAvailable() {
        try {
            Profile.Feature ssfFeature = Profile.Feature.valueOf("SSF");
            return Profile.isFeatureEnabled(ssfFeature);
        } catch (IllegalArgumentException | NoClassDefFoundError e) {
            logger.debug("SSF feature not available in this Keycloak version");
            return false;
        }
    }
}
