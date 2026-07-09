package io.github.mabartos.evaluator.ip;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class IpWhitelistRiskEvaluatorFactory implements RiskEvaluatorFactory {

    public static final String PROVIDER_ID = "ip-whitelist-risk-evaluator";
    public static final String NAME = "IP whitelist";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Reduces risk when the client IP matches a configured IPv4 whitelist (single address or range).";
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return IpWhitelistRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new IpWhitelistRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalAdminConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(IpWhitelistEvaluatorConfig.WHITELIST_IPV4_CONFIG)
                .label("IPv4 whitelist")
                .helpText("Comma-separated IPv4 entries: single address, hyphen range (10.0.0.1-10.0.0.255), "
                        + "or CIDR (10.0.0.0/8). Invalid entries are ignored at runtime.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(IpWhitelistEvaluatorConfig.WHITELISTED_SCORE_CONFIG)
                .label("Whitelisted IP score")
                .helpText("Risk score when the client IP matches the whitelist (trust signal).")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(IpWhitelistEvaluatorConfig.configurableScoreNames())
                .defaultValue(IpWhitelistEvaluatorConfig.DEFAULT_WHITELISTED_SCORE.name())
                .add()
                .property()
                .name(IpWhitelistEvaluatorConfig.NOT_WHITELISTED_SCORE_CONFIG)
                .label("Non-whitelisted IP score")
                .helpText("Risk score when the client IP does not match the whitelist.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(IpWhitelistEvaluatorConfig.configurableScoreNames())
                .defaultValue(IpWhitelistEvaluatorConfig.DEFAULT_NOT_WHITELISTED_SCORE.name())
                .add()
                .build();
    }
}
