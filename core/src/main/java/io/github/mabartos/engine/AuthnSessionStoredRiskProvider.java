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
package io.github.mabartos.engine;

import io.github.mabartos.spi.engine.StoredRiskProperties;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.ResultRisk;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Provider for storing risk scores in authentication session attributes
 */
public class AuthnSessionStoredRiskProvider implements StoredRiskProvider {
    private static final Logger logger = Logger.getLogger(AuthnSessionStoredRiskProvider.class);
    private final KeycloakSession session;

    public AuthnSessionStoredRiskProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ResultRisk getStoredOverallRisk() {
        return getStoredRisk(StoredRiskProperties.getOverallScoreProperty(), StoredRiskProperties.getOverallSummaryProperty());
    }

    @Override
    public ResultRisk getStoredRisk(@Nonnull RiskEvaluator.EvaluationPhase phase) {
        return getStoredRisk(StoredRiskProperties.getOverallScoreProperty(phase), StoredRiskProperties.getOverallSummaryProperty(phase));
    }

    private ResultRisk getStoredRisk(@Nonnull String scoreProperty, @Nonnull String reasonProperty) {
        return getAdditionalData(scoreProperty)
                .filter(score -> StringUtil.isNotBlank(score) && !score.equals(Double.toString(ResultRisk.invalid().getScore())))
                .map(score -> {
                    try {
                        var reason = getAdditionalData(reasonProperty).orElse("");
                        return ResultRisk.of(Double.parseDouble(score), reason);
                    } catch (NumberFormatException e) {
                        return ResultRisk.invalid();
                    }
                }).filter(ResultRisk::isValid)
                .orElse(ResultRisk.invalid("No stored risk"));
    }

    @Override
    public void storeOverallRisk(@Nonnull ResultRisk risk) {
        storeRisk(risk, StoredRiskProperties.getOverallScoreProperty(), StoredRiskProperties.getOverallSummaryProperty());
    }

    @Override
    public void storeRisk(@Nonnull ResultRisk risk, @Nonnull RiskEvaluator.EvaluationPhase phase) {
        storeRisk(risk, StoredRiskProperties.getOverallScoreProperty(phase), StoredRiskProperties.getOverallSummaryProperty(phase));
    }

    private void storeRisk(@Nonnull ResultRisk risk, @Nonnull String scoreProperty, @Nonnull String reasonProperty) {
        if (!risk.isValid()) {
            logger.warnf("Cannot store the invalid risk score '%f'", risk);
            return;
        }

        storeAdditionalData(scoreProperty, Double.toString(risk.getScore()));
        risk.getSummary().ifPresent(summary -> storeAdditionalData(reasonProperty, summary));
    }

    @Override
    public void storeAdditionalData(String key, String value) {
        getAuthnSession().ifPresentOrElse(
                authnSession -> authnSession.setAuthNote(key, value),
                () -> logger.debugf("Skipping store of '%s': no authentication session available", key));
    }

    @Override
    public Optional<String> getAdditionalData(String key) {
        return getAuthnSession().map(authnSession -> authnSession.getAuthNote(key));
    }

    protected Optional<AuthenticationSessionModel> getAuthnSession() {
        return Optional.ofNullable(session.getContext().getAuthenticationSession());
    }

    @Override
    public void close() {

    }
}
