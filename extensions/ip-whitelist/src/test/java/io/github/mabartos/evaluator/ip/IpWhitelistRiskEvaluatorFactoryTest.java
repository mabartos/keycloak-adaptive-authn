package io.github.mabartos.evaluator.ip;

import org.junit.jupiter.api.Test;
import org.keycloak.provider.ProviderConfigProperty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IpWhitelistRiskEvaluatorFactoryTest {

    private final IpWhitelistRiskEvaluatorFactory factory = new IpWhitelistRiskEvaluatorFactory();

    @Test
    void additionalAdminConfigProperties_exposeWhitelistAndScores() {
        var properties = factory.getAdditionalAdminConfigProperties();

        assertEquals(3, properties.size());
        assertThat(properties.get(0).getName(), is(IpWhitelistEvaluatorConfig.WHITELIST_IPV4_CONFIG));
        assertThat(properties.get(0).getType(), is(ProviderConfigProperty.STRING_TYPE));
        assertThat(properties.get(1).getName(), is(IpWhitelistEvaluatorConfig.WHITELISTED_SCORE_CONFIG));
        assertThat(properties.get(1).getType(), is(ProviderConfigProperty.LIST_TYPE));
        assertThat(properties.get(2).getName(), is(IpWhitelistEvaluatorConfig.NOT_WHITELISTED_SCORE_CONFIG));
        assertThat(properties.get(2).getType(), is(ProviderConfigProperty.LIST_TYPE));
    }
}
