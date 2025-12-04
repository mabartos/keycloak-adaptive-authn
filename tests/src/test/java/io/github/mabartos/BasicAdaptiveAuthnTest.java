package io.github.mabartos;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest(config = BasicAdaptiveAuthnTest.Config.class)
public class BasicAdaptiveAuthnTest {

    @Test
    public void executeServerInEmbeddedMode() throws InterruptedException {
        assertThat(true, CoreMatchers.is(true));
        //Thread.sleep(99999999);
    }

    public static class Config implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn");
        }
    }
}
