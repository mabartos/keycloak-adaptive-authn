package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultRiskEngine implements RiskEngine {
    private static final Logger logger = Logger.getLogger(DefaultRiskEngine.class);

    private final KeycloakSession session;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private boolean requiresUser;
    private Double risk;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.riskFactorEvaluators = session.getAllProviders(RiskEvaluator.class);
    }

    @Override
    public void evaluateRisk() {
        logger.debugf("Risk Engine - EVALUATING");

        final var riskEvaluator = getRiskEvaluators()
                .stream()
                .filter(f -> f.requiresUser() == this.requiresUser)
                .toList();

        riskEvaluator.forEach(RiskEvaluator::evaluate);

        var filteredEvaluators = riskEvaluator.stream()
                .filter(f -> RiskEngine.isValidValue(f.getWeight()))
                .filter(f -> RiskEngine.isValidValue(f.getRiskValue()))
                .toList();

        var weightedRisk = filteredEvaluators.stream()
                .peek(f -> logger.debugf("Evaluator: %s", f.getClass().getSimpleName()))
                .peek(f -> logger.debugf("Risk evaluated: %f (weight %f)", f.getRiskValue(), f.getWeight()))
                .mapToDouble(f -> f.getRiskValue() * f.getWeight())
                .sum();
        var weights = filteredEvaluators
                .stream()
                .mapToDouble(RiskEvaluator::getWeight)
                .sum();

        // Weighted mean
        this.risk = weightedRisk / weights;
        logger.debugf("The overall risk score is %f - (requires user: %s)", risk, requiresUser);
    }

    @Override
    public Double getRisk() {
        return risk;
    }

    @Override
    public Set<UserContext<?>> getRiskFactors() {
        return riskFactorEvaluators.stream()
                .flatMap(f -> f.getUserContexts().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var requiresUser = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(DefaultRiskEngineFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(null);

        if (requiresUser == null) {
            logger.warnf("Cannot find config '%s'", DefaultRiskEngineFactory.REQUIRES_USER_CONFIG);
            return;
        }
        this.requiresUser = requiresUser;

        evaluateRisk();
        storeRisk(context, requiresUser ? RiskPhase.REQUIRES_USER : RiskPhase.NO_USER);
        context.success();
    }

    public enum RiskPhase {
        NO_USER(RISK_NO_USER_AUTH_NOTE),
        REQUIRES_USER(RISK_REQUIRES_USER_AUTH_NOTE),
        OVERALL(RISK_OVERALL_AUTH_NOTE);

        final String authNote;

        RiskPhase(String authNote) {
            this.authNote = authNote;
        }

        public String getAuthNote() {
            return authNote;
        }
    }

    public static Optional<Double> getStoredRisk(AuthenticationFlowContext context, RiskPhase riskPhase) {
        try {
            return Optional.ofNullable(context.getAuthenticationSession())
                    .map(f -> f.getAuthNote(riskPhase.getAuthNote()))
                    .filter(StringUtil::isNotBlank)
                    .map(Double::parseDouble);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static void storeRisk(AuthenticationFlowContext context, RiskPhase riskPhase, Double risk) {
        context.getAuthenticationSession().setAuthNote(riskPhase.getAuthNote(), risk.toString());

        if (riskPhase != RiskPhase.OVERALL) { // Store Overall risk
            var oppositePhase = riskPhase == RiskPhase.NO_USER ? RiskPhase.REQUIRES_USER : RiskPhase.NO_USER;
            getStoredRisk(context, oppositePhase)
                    .ifPresent(oppositeRisk -> {
                        final var sum = risk + oppositeRisk;
                        final var result = sum / 2.0f;

                        logger.debugf("Stored overall risk: %f ('%s') + %f ('%s') = %f / 2.0 = %f", risk, riskPhase.name(), oppositeRisk, oppositePhase.name(), sum, result);

                        context.getAuthenticationSession().setAuthNote(RiskPhase.OVERALL.getAuthNote(), Double.toString(result));
                    });
        }
    }

    public void storeRisk(AuthenticationFlowContext context, RiskPhase riskPhase) {
        storeRisk(context, riskPhase, getRisk());
    }
}