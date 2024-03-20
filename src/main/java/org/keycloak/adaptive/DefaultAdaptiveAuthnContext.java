package org.keycloak.adaptive;

import org.keycloak.adaptive.spi.AdaptiveAuthnContext;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class DefaultAdaptiveAuthnContext implements AdaptiveAuthnContext {
    private final KeycloakSession session;

    private Double riskValue;
    private Set<UserContext<?>> userContexts = Collections.emptySet();
    private RiskLevel currentRiskLevel;

    public DefaultAdaptiveAuthnContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Double getCurrentRisk() {
        return riskValue;
    }

    @Override
    public void setRisk(Double riskValue) {
        this.riskValue = riskValue;
    }

    @Override
    public Collection<UserContext<?>> getUsedUserContexts() {
        return userContexts;
    }

    @Override
    public void setUsedUserContexts(Set<UserContext<?>> userContexts) {
        this.userContexts = userContexts;
    }

    @Override
    public RiskLevel getCurrentRiskLevel() {
        return currentRiskLevel;
    }

    @Override
    public void setCurrentRiskLevel(RiskLevel level) {
        this.currentRiskLevel = level;
    }
}
