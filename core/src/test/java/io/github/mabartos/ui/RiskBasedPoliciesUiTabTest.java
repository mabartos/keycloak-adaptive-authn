package io.github.mabartos.ui;

import io.github.mabartos.evaluator.behavior.ConcurrentSessionRiskEvaluatorFactory;
import io.github.mabartos.evaluator.browser.BrowserRiskEvaluatorFactory;
import io.github.mabartos.evaluator.client.ClientSensitivityRiskEvaluatorFactory;
import io.github.mabartos.evaluator.role.DefaultUserRoleEvaluatorFactory;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.junit.jupiter.api.Test;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskBasedPoliciesUiTabTest {

    @Test
    void buildConfigProperties_ordersEvaluatorsByPhase() {
        var factories = List.<RiskEvaluatorFactory>of(
                new ConcurrentSessionRiskEvaluatorFactory(),
                new BrowserRiskEvaluatorFactory(),
                new DefaultUserRoleEvaluatorFactory());

        var props = RiskBasedPoliciesUiTab.buildConfigProperties(List.of(), factories);
        var labels = evaluatorEnabledLabels(props);

        int beforeAuthn = indexOfPhase(labels, "[BEFORE_AUTHN]");
        int userKnown = indexOfPhase(labels, "[USER_KNOWN]");
        int continuous = indexOfPhase(labels, "[CONTINUOUS]");

        assertTrue(beforeAuthn >= 0);
        assertTrue(userKnown > beforeAuthn);
        assertTrue(continuous > userKnown);
    }

    @Test
    void buildConfigProperties_sortsWithinPhaseByAdminDisplayName() {
        var factories = List.<RiskEvaluatorFactory>of(
                new ClientSensitivityRiskEvaluatorFactory(),
                new BrowserRiskEvaluatorFactory());

        var labels = evaluatorEnabledLabels(
                RiskBasedPoliciesUiTab.buildConfigProperties(List.of(), factories));

        assertEquals(2, labels.size());
        assertTrue(labels.get(0).contains("Browser"));
        assertTrue(labels.get(1).contains("Client sensitivity"));
    }

    @Test
    void buildConfigProperties_labelsIncludePhasePrefix() {
        var factories = List.<RiskEvaluatorFactory>of(new BrowserRiskEvaluatorFactory());

        var enabled = RiskBasedPoliciesUiTab.buildConfigProperties(List.of(), factories).stream()
                .filter(p -> p.getName().startsWith("adaptive-evaluator-enabled-"))
                .findFirst()
                .orElseThrow();

        assertEquals("[BEFORE_AUTHN] Browser", enabled.getLabel());

        var trust = RiskBasedPoliciesUiTab.buildConfigProperties(List.of(), factories).stream()
                .filter(p -> p.getName().startsWith("adaptive-evaluator-trust-"))
                .findFirst()
                .orElseThrow();

        assertEquals("[BEFORE_AUTHN] Browser trust", trust.getLabel());
    }

    private static List<String> evaluatorEnabledLabels(List<ProviderConfigProperty> props) {
        return props.stream()
                .filter(p -> p.getName() != null && p.getName().startsWith("adaptive-evaluator-enabled-"))
                .map(ProviderConfigProperty::getLabel)
                .toList();
    }

    private static int indexOfPhase(List<String> labels, String phasePrefix) {
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).startsWith(phasePrefix)) {
                return i;
            }
        }
        return -1;
    }
}
