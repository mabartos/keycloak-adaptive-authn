package io.github.mabartos.evaluator.recaptcha;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class RecaptchaRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "recaptcha-enterprise";
    public static final String NAME = "reCAPTCHA Enterprise";

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
