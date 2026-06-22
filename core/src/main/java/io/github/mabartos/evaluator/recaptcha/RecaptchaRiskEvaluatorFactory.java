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

    @Override
    public String adminDisplayName() {
        return "reCAPTCHA";
    }

    @Override
    public String adminEnabledHelpText() {
        return "Uses Google reCAPTCHA Enterprise risk scores for the login attempt (requires reCAPTCHA integration).";
    }

    @Override
    public String adminTrustHelpText() {
        return "Typically runs as part of the authentication flow; disable only if reCAPTCHA is not configured.";
    }
}
