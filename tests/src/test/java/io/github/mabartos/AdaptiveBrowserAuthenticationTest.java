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
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.openqa.selenium.support.PageFactory;

import static org.junit.jupiter.api.Assertions.*;

@KeycloakIntegrationTest(config = AdaptiveBrowserAuthenticationTest.Config.class)
public class AdaptiveBrowserAuthenticationTest {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive")
    ManagedRealm adaptiveRealm;

    @InjectOAuthClient(realmRef = "adaptive")
    OAuthClient oauthClient;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @Test
    @Disabled("Password hash mismatch - realm JSON contains hashed password")
    public void testUserCanLoginViaBrowser() {
        // Use doLogin which handles the full browser-based login flow
        AuthorizationEndpointResponse response = oauthClient.doLogin("user", "user");

        // Verify successful login - should have authorization code
        assertNotNull(response, "Authorization response should not be null");
        assertNotNull(response.getCode(), "Authorization code should not be null");
        assertNull(response.getError(), "There should be no error");
    }

    @Test
    @Disabled("Password hash mismatch - realm JSON contains hashed password")
    public void testUserCannotLoginWithInvalidPasswordViaBrowser() {
        // Initiate OAuth authorization flow
        oauthClient.openLoginForm();

        // Wait for login page and fill in form with invalid password
        PageFactory.initElements(webDriver.driver(), loginPage);
        loginPage.assertCurrent();
        loginPage.fillLogin("user", "wrongpassword");
        loginPage.submit();

        // Verify login failed - should show error message
        String errorMessage = loginPage.getUsernameInputError();
        assertNotNull(errorMessage, "Error message should be displayed");
    }

    @Test
    public void testAdaptiveFlowExecutesDuringBrowserLogin() {
        // This test verifies that the adaptive authentication flow is triggered
        // during browser-based login

        // Initiate OAuth authorization flow
        oauthClient.openLoginForm();

        // Login page should be displayed - this means the adaptive flow executed
        // and presented the username/password form
        PageFactory.initElements(webDriver.driver(), loginPage);
        loginPage.assertCurrent();

        // Complete login
        loginPage.fillLogin("user", "user");
        loginPage.submit();

        // Wait for OAuth callback and parse response
        webDriver.waiting().waitForOAuthCallback();
        AuthorizationEndpointResponse response = oauthClient.parseLoginResponse();

        // Verify successful authentication through adaptive flow
        assertNotNull(response.getCode(), "Should have authorization code after successful adaptive authentication");
        assertNull(response.getError(), "Should have no errors");
    }

    // Server configuration
    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            builder.log().categoryLevel("io.github.mabartos.keycloak.authentication", "trace");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("tracing-enabled", "true");
        }
    }
}
