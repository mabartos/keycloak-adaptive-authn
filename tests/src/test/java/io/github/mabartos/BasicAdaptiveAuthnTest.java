package io.github.mabartos;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@KeycloakIntegrationTest(config = BasicAdaptiveAuthnTest.Config.class)
public class BasicAdaptiveAuthnTest {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive")
    ManagedRealm adaptiveRealm;

    @Test
    public void base() {
        assertThat(adaptiveRealm, notNullValue());
        assertThat(adaptiveRealm.getName(), is("adaptive"));
        var representation = adaptiveRealm.getCreatedRepresentation();
        assertThat(representation, notNullValue());
        var browserFlow = representation.getBrowserFlow();
        assertThat(browserFlow, is("adaptive"));

        var adaptiveFlow = adaptiveRealm.admin().flows()
                .getFlows()
                .stream()
                .filter(f -> f.getAlias().equals("adaptive"))
                .findFirst()
                .orElse(null);
        assertThat(adaptiveFlow, notNullValue());
        /*adaptiveFlow = adaptiveRealm.admin().flows().getFlow(adaptiveRealm.getId());
        assertThat(adaptiveFlow, notNullValue());

        var adaptiveFlowExecutions = adaptiveFlow.getAuthenticationExecutions();
        assertThat(adaptiveFlowExecutions, notNullValue());
        assertThat(adaptiveFlowExecutions.size() > 1, is(true));*/
    }

    @Test
    public void executeServerInEmbeddedMode() throws InterruptedException {
        if (Boolean.parseBoolean(System.getProperty("kc.adaptive.test.embedded", "false"))) {
            Thread.sleep(99999999);
        }
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("tracing-enabled", "true");
        }
    }
}
