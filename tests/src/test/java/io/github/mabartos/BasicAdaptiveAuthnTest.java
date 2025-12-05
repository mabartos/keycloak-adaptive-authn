package io.github.mabartos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.io.IOException;
import java.io.InputStream;

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
        //Thread.sleep(99999999);
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("tracing-enabled", "true");
        }
    }

    public static class AdaptiveRealmConfig implements RealmConfig {
        public static final String REALM_JSON_NAME = "test-adaptive-realm.json";

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(REALM_JSON_NAME)) {
                if (is == null) {
                    throw new RuntimeException(REALM_JSON_NAME + " not found in classpath");
                }

                ObjectMapper mapper = new ObjectMapper();
                RealmRepresentation realmRep = mapper.readValue(is, RealmRepresentation.class);

                return RealmConfigBuilder.update(realmRep);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load " + REALM_JSON_NAME, e);
            }
        }
    }
}
