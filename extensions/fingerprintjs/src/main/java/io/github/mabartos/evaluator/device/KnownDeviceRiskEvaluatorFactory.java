package io.github.mabartos.evaluator.device;

import io.github.mabartos.context.device.KnownDeviceContext;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class KnownDeviceRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "known-device-risk-evaluator";
    public static final String NAME = "Known Device";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Compares the browser device fingerprint to the user's known devices after identification. "
                + "Requires the 'Device fingerprint collector' execution in the browser authentication flow.";
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return KnownDeviceRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new KnownDeviceRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalAdminConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(KnownDeviceContext.TTL_DAYS_CONFIG)
                .label("TTL (days)")
                .helpText("Number of days before a known device stops providing a trust signal. "
                        + "Expired entries are removed on successful login. Set to 0 to disable expiration.")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(KnownDeviceContext.DEFAULT_TTL_DAYS)
                .add()
                .property()
                .name(KnownDeviceContext.MAX_STORED_DEVICES_CONFIG)
                .label("Max stored devices")
                .helpText("Maximum number of known devices kept per user. "
                        + "When the limit is exceeded, the oldest devices are dropped on successful login. "
                        + "Minimum 1; invalid or values below 1 fall back to "
                        + KnownDeviceContext.DEFAULT_MAX_STORED_DEVICES + ".")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(KnownDeviceContext.DEFAULT_MAX_STORED_DEVICES)
                .add()
                .build();
    }
}
