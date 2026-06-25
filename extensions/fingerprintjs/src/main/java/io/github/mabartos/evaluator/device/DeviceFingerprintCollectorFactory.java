package io.github.mabartos.evaluator.device;

import io.github.mabartos.context.device.KnownDeviceConstants;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class DeviceFingerprintCollectorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = KnownDeviceConstants.COLLECTOR_PROVIDER_ID;

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return new DeviceFingerprintCollector();
    }

    @Override
    public String getDisplayType() {
        return "Device fingerprint collector";
    }

    @Override
    public String getReferenceCategory() {
        return "Device fingerprint collector";
    }

    @Override
    public String getHelpText() {
        return "Collects a browser device fingerprint using FingerprintJS before risk evaluation";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
