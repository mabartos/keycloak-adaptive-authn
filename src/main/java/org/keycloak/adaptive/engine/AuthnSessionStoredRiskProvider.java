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
package org.keycloak.adaptive.engine;

import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Provider for storing risk scores in authentication session attributes
 */
public class AuthnSessionStoredRiskProvider implements StoredRiskProvider {
    private static final Logger logger = Logger.getLogger(AuthnSessionStoredRiskProvider.class);

    private static final String OVERALL_PROP = "OVERALL";
    protected static final String ADAPTIVE_AUTHN_RISK_SCORE_PREFIX = "ADAPTIVE_AUTHN_CURRENT_RISK_SCORE_";
    protected static final String ADAPTIVE_AUTHN_RISK_REASON_PREFIX = "ADAPTIVE_AUTHN_CURRENT_RISK_REASON_";

    private final KeycloakSession session;

    public AuthnSessionStoredRiskProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Risk getStoredOverallRisk() {
        return getStoredRisk(null);
    }

    @Override
    public Risk getStoredRisk(@Nullable RiskEvaluator.EvaluationPhase phase) {
        try {
            return Optional.ofNullable(session.getContext().getAuthenticationSession())
                    .map(f -> {
                        var score = f.getAuthNote(getScoreProperty(phase));
                        if (StringUtil.isBlank(score) || score.equals("-1")) {
                            return Risk.invalid();
                        }
                        var reason = f.getAuthNote(getReasonProperty(phase));
                        return Risk.of(Double.parseDouble(score), reason);
                    })
                    .filter(Risk::isValid)
                    .orElse(Risk.invalid());
        } catch (NumberFormatException e) {
            return Risk.invalid();
        }
    }

    @Override
    public void storeOverallRisk(Risk risk) {
        storeRisk(risk, null);
    }

    @Override
    public void storeRisk(Risk risk, @Nullable RiskEvaluator.EvaluationPhase phase) {
        if (!risk.isValid()) {
            logger.warnf("Cannot store the invalid risk score '%f'", risk);
            return;
        }

        Optional.ofNullable(session.getContext().getAuthenticationSession())
                .ifPresentOrElse(f -> {
                            f.setAuthNote(getScoreProperty(phase), Double.toString(risk.getScore().get()));
                            f.setAuthNote(getReasonProperty(phase), risk.getReason().orElse(""));
                        },
                        () -> {
                            throw new IllegalStateException("Authentication session is null");
                        });

        // Store Overall risk
        if (phase != null && !phase.equals(RiskEvaluator.EvaluationPhase.CONTINUOUS)) {
            var oppositePhase = phase == RiskEvaluator.EvaluationPhase.BEFORE_AUTHN ?
                    RiskEvaluator.EvaluationPhase.USER_KNOWN :
                    RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;
            var oppositeRisk = getStoredRisk(oppositePhase);
            var overallRisk = risk;

            if (oppositeRisk.isValid()) {
                overallRisk = risk.getScore().get() > oppositeRisk.getScore().get() ? risk : oppositeRisk;
                logger.debugf("Stored overall risk: max(%f ('%s'), %f ('%s')) = %f", risk.getScore().get(), phase.name(), oppositeRisk.getScore().get(), oppositePhase.name(), overallRisk.getScore().get());
            } else {
                logger.tracef("Stored overall risk: %f ('%s')", risk.getScore().get(), phase.name());
            }

            storeOverallRisk(overallRisk);
        }
    }

    static String getScoreProperty(@Nullable RiskEvaluator.EvaluationPhase phase) {
        return ADAPTIVE_AUTHN_RISK_SCORE_PREFIX + Optional.ofNullable(phase).map(Enum::name).orElse(OVERALL_PROP);
    }

    static String getReasonProperty(@Nullable RiskEvaluator.EvaluationPhase phase) {
        return ADAPTIVE_AUTHN_RISK_REASON_PREFIX + Optional.ofNullable(phase).map(Enum::name).orElse(OVERALL_PROP);
    }

    @Override
    public void close() {

    }
}
