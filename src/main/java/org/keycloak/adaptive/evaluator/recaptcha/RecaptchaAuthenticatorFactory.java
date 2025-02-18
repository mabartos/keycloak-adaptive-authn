package org.keycloak.adaptive.evaluator.recaptcha;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class RecaptchaAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = RecaptchaRiskEvaluatorFactory.PROVIDER_ID;
    public static final String SITE_KEY = "site.key";
    public static final String PROJECT_ID = "project.id";
    public static final String API_KEY = "api.key";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new RecaptchaRiskEvaluator(session);
    }

    @Override
    public String getDisplayType() {
        return "ReCAPTCHA Enterprise risk evaluator";
    }

    @Override
    public String getReferenceCategory() {
        return "ReCAPTCHA Enterprise risk evaluator";
    }

    @Override
    public String getHelpText() {
        return "Obtain risk score from reCAPTCHA Enterprise";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(PROJECT_ID)
                .label("Project ID")
                .helpText("Project ID the site key belongs to.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SITE_KEY)
                .label("reCAPTCHA Site Key")
                .helpText("The site key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(API_KEY)
                .label("Google API Key")
                .helpText("An API key with the reCAPTCHA Enterprise API enabled in the given project ID.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(true)
                .add()
                .build();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
