package org.keycloak.adaptive.evaluator.recaptcha;

import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class RecaptchaRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "recaptcha-v3";
    public static final String NAME = "reCAPTCHA v3";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return RecaptchaRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new RecaptchaRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
