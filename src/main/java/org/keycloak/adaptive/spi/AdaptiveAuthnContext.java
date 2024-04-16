package org.keycloak.adaptive.spi;

import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.adaptive.spi.level.RiskLevel;

import java.util.Collection;
import java.util.Set;

public interface AdaptiveAuthnContext {

    Double getCurrentRisk();

    void setRisk(Double riskValue);

    Collection<UserContext<?>> getUsedUserContexts();

    void setUsedUserContexts(Set<UserContext<?>> userContexts);

    RiskLevel getCurrentRiskLevel();

    void setCurrentRiskLevel(RiskLevel level);
}
