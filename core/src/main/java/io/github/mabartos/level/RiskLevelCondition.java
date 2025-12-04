/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.level;

import org.jboss.logging.Logger;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.level.RiskLevelsProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Condition for checking the evaluated overall risk score
 */
public class RiskLevelCondition implements ConditionalAuthenticator {
    private static final Logger logger = Logger.getLogger(RiskLevelCondition.class);

    private final RiskLevelsProvider riskLevelsProvider;

    public RiskLevelCondition(RiskLevelsProvider riskLevelsProvider) {
        this.riskLevelsProvider = riskLevelsProvider;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();

        if (authConfig != null) {
            if (riskLevelsProvider == null) {
                logger.errorf("Cannot find risk level provider");
                throw new IllegalStateException("Risk Level Provider is not found");
            }

            var storedRiskProvider = context.getSession().getProvider(StoredRiskProvider.class);
            var risk = Optional.of(storedRiskProvider.getStoredOverallRisk())
                    .filter(Risk::isValid)
                    .orElseThrow(() -> new IllegalStateException("No risk has been evaluated or invalid risk score. Did you forget to add Risk Engine authenticator to the flow?"));


            var level = Optional.ofNullable(authConfig.getConfig().get(AbstractRiskLevelConditionFactory.LEVEL_CONFIG))
                    .filter(StringUtil::isNotBlank)
                    .flatMap(f -> riskLevelsProvider.getRiskLevels().stream().filter(g -> g.getName().equals(f)).findAny())
                    .orElseThrow(() -> new IllegalStateException("Cannot find specified level for provider: " + riskLevelsProvider));

            var matches = level.matchesRisk(risk.getScore().get());

            if (matches) {
                logger.debugf("Risk Level Condition (%s) matches the evaluated level: %f < %f <= %f", level.getName(), level.getLowestRiskValue(), risk.getScore().get(), level.getHighestRiskValue());
                return true;
            } else {
                logger.tracef("Risk Level Condition (%s) DOES NOT MATCH the evaluated level: %f", level.getName(), risk.getScore().get());
                return false;
            }
        }
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
