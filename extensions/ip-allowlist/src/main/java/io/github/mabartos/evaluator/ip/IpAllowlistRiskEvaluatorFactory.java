package io.github.mabartos.evaluator.ip;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class IpAllowlistRiskEvaluatorFactory implements RiskEvaluatorFactory {

    public static final String PROVIDER_ID = "ip-allowlist-risk-evaluator";
    public static final String NAME = "IP allowlist";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Reduces risk when the client IP matches a configured IPv4 allowlist (single address or range).";
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return IpAllowlistRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new IpAllowlistRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalAdminConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(IpAllowlistEvaluatorConfig.ALLOWLIST_IPV4_CONFIG)
                .label("IPv4 allowlist")
                .helpText("Comma-separated IPv4 entries: single address, hyphen range (10.0.0.1-10.0.0.255), "
                        + "or CIDR (10.0.0.0/8). Invalid entries are ignored at runtime.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(IpAllowlistEvaluatorConfig.ALLOWLISTED_SCORE_CONFIG)
                .label("Allowlisted IP score")
                .helpText("Risk score when the client IP matches the allowlist (trust signal).")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(IpAllowlistEvaluatorConfig.configurableScoreNames())
                .defaultValue(IpAllowlistEvaluatorConfig.DEFAULT_ALLOWLISTED_SCORE.name())
                .add()
                .property()
                .name(IpAllowlistEvaluatorConfig.NOT_ALLOWLISTED_SCORE_CONFIG)
                .label("Non-allowlisted IP score")
                .helpText("Risk score when the client IP does not match the allowlist.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(IpAllowlistEvaluatorConfig.configurableScoreNames())
                .defaultValue(IpAllowlistEvaluatorConfig.DEFAULT_NOT_ALLOWLISTED_SCORE.name())
                .add()
                .build();
    }
}
