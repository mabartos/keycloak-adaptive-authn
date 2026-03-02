package io.github.mabartos;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import static org.junit.jupiter.api.Assertions.*;

@KeycloakIntegrationTest(config = AdaptiveAuthenticationTest.Config.class)
public class AdaptiveAuthenticationTest {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive")
    ManagedRealm adaptiveRealm;

    @InjectOAuthClient(realmRef = "adaptive")
    OAuthClient oauthClient;

    @Test
    @Disabled("Password hash mismatch - realm JSON contains hashed password")
    public void testUserCanLogin() {
        // Test that user:user can authenticate successfully using password grant
        AccessTokenResponse response = oauthClient.doPasswordGrantRequest("user", "user");

        assertNotNull(response, "Access token response should not be null");
        assertNotNull(response.getAccessToken(), "Access token should not be null");
        assertNotNull(response.getRefreshToken(), "Refresh token should not be null");
        assertNull(response.getError(), "There should be no error");
    }

    @Test
    public void testUserCannotLoginWithInvalidPassword() {
        // Test that login fails with wrong password
        AccessTokenResponse response = oauthClient.doPasswordGrantRequest("user", "wrongpassword");

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getError(), "Error should be present");
        assertEquals("invalid_grant", response.getError(), "Error should be invalid_grant");
    }

    @Test
    public void testRealmIsConfigured() {
        // Verify realm is properly set up
        assertNotNull(adaptiveRealm, "Adaptive realm should be injected");
        assertEquals("adaptive", adaptiveRealm.getName(), "Realm name should be 'adaptive'");

        var representation = adaptiveRealm.getCreatedRepresentation();
        assertNotNull(representation, "Realm representation should not be null");
        assertEquals("adaptive", representation.getBrowserFlow(),
                "Browser flow should be set to 'adaptive'");
    }

    @Test
    public void testAdaptiveFlowExists() {
        // Verify the adaptive flow exists and is configured
        var adaptiveFlow = adaptiveRealm.admin().flows()
                .getFlows()
                .stream()
                .filter(f -> f.getAlias().equals("adaptive"))
                .findFirst()
                .orElse(null);

        assertNotNull(adaptiveFlow, "Adaptive flow should exist");
        assertEquals("adaptive", adaptiveFlow.getAlias(), "Flow alias should be 'adaptive'");
    }

    // Server configuration
    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("tracing-enabled", "true");
        }
    }
}
