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

import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.Optional;

/**
 * Condition for checking the evaluated overall risk score.
 * Gets risk levels directly from the RiskScoreAlgorithm being used.
 */
public class RiskLevelCondition implements ConditionalAuthenticator {
    private static final Logger logger = Logger.getLogger(RiskLevelCondition.class);

    private final boolean isAdvanced;

    public RiskLevelCondition(boolean isAdvanced) {
        this.isAdvanced = isAdvanced;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();

        if (authConfig != null) {
            // Check if risk-based authentication is enabled
            var riskEngine = context.getSession().getProvider(RiskEngine.class);
            if (riskEngine != null && !riskEngine.isRiskBasedAuthnEnabled()) {
                logger.warn("Risk-based authentication is disabled. Skipping risk level condition check.");
                return false;
            }

            if (riskEngine == null) {
                logger.errorf("RiskEngine not available");
                throw new IllegalStateException("RiskEngine not found");
            }

            RiskScoreAlgorithm algorithm = riskEngine.getRiskScoreAlgorithm();
            if (algorithm == null) {
                logger.errorf("RiskScoreAlgorithm not available");
                throw new IllegalStateException("RiskScoreAlgorithm not found");
            }

            // Get the appropriate risk levels (simple or advanced) from the algorithm
            List<RiskLevel> riskLevels = isAdvanced
                ? algorithm.getAdvancedRiskLevels().getLevels()
                : algorithm.getSimpleRiskLevels().getLevels();

            String description = isAdvanced ? AdvancedRiskLevels.getDescription() : SimpleRiskLevels.getDescription();
            logger.debugf("Using %s risk levels from algorithm: %s", description, algorithm.getClass().getSimpleName());

            var storedRiskProvider = context.getSession().getProvider(StoredRiskProvider.class);
            var risk = Optional.of(storedRiskProvider.getStoredOverallRisk())
                    .filter(ResultRisk::isValid)
                    .orElseThrow(() -> new IllegalStateException("No risk has been evaluated or invalid risk score. Did you forget to add Risk Engine authenticator to the flow?"));


            var level = Optional.ofNullable(authConfig.getConfig().get(AbstractRiskLevelConditionFactory.LEVEL_CONFIG))
                    .filter(StringUtil::isNotBlank)
                    .flatMap(f -> riskLevels.stream().filter(g -> g.name().equals(f)).findAny())
                    .orElseThrow(() -> new IllegalStateException("Cannot find specified level: " + authConfig.getConfig().get(AbstractRiskLevelConditionFactory.LEVEL_CONFIG)));

            var matches = level.matchesRisk(risk.getScore());

            if (matches) {
                logger.debugf("Risk Level Condition (%s) matches the evaluated level: %f < %f <= %f", level.name(), level.lowestRiskValue(), risk.getScore(), level.highestRiskValue());
                return true;
            } else {
                logger.tracef("Risk Level Condition (%s) DOES NOT MATCH the evaluated level: %f", level.name(), risk.getScore());
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
