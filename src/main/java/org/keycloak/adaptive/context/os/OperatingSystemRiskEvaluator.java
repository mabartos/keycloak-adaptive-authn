package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;

public class OperatingSystemRiskEvaluator implements RiskEvaluator {
    private final KeycloakSession session;
    private final OperatingSystemCondition condition;
    private Double risk;

    public OperatingSystemRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.condition = ContextUtils.getContextCondition(session, OperatingSystemConditionFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public void evaluate() {
        if (condition.isOs(DefaultOperatingSystems.LINUX)) {
            this.risk = Risk.INTERMEDIATE; // they say that the probability of attacks is higher from Linux devices xD
        } else if (condition.isDefaultKnownOs()) {
            this.risk = Risk.SMALL;
        } else {
            this.risk = Risk.INTERMEDIATE;
        }
    }
}
