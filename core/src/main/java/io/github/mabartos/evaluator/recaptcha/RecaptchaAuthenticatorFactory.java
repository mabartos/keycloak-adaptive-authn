package io.github.mabartos.evaluator.recaptcha;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.List;
import java.util.Optional;

public class RecaptchaAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = RecaptchaRiskEvaluatorFactory.PROVIDER_ID;
    public static final String SITE_KEY_CONSOLE = "site.key";
    public static final String PROJECT_ID_CONSOLE = "project.id";
    public static final String API_KEY_CONSOLE = "api.key";

    private static final String SITE_KEY_PROPERTY = "recaptcha.site.key";
    private static final String PROJECT_ID_PROPERTY = "recaptcha.project.id";
    private static final String PROJECT_API_KEY_PROPERTY = "recaptcha.project.api.key";

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
                .name(PROJECT_ID_CONSOLE)
                .label("Project ID")
                .helpText("Project ID the site key belongs to.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SITE_KEY_CONSOLE)
                .label("reCAPTCHA Site Key")
                .helpText("The site key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(API_KEY_CONSOLE)
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

    public static Optional<String> getSiteKey() {
        return Configuration.getOptionalValue(SITE_KEY_PROPERTY);
    }

    public static Optional<String> getProjectId() {
        return Configuration.getOptionalValue(PROJECT_ID_PROPERTY);
    }

    public static Optional<String> getProjectApiKey() {
        return Configuration.getOptionalValue(PROJECT_API_KEY_PROPERTY);
    }
}
