package org.keycloak.adaptive.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import java.util.List;

class KeycloakAdaptiveAuthnProcessor {

    private static final String FEATURE = "keycloak-adaptive-authn";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Consume(FeatureBuildItem.class)
    void listFeatures(List<FeatureBuildItem> list) {
        System.err.println("HERE");
        System.err.println(list);
    }
}