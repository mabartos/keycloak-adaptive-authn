package io.github.mabartos.keycloak.adaptive.authn.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class KeycloakAdaptiveAuthnProcessor {

    private static final String FEATURE = "keycloak-adaptive-authn";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}