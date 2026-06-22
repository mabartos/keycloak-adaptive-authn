package io.github.mabartos.evaluator.login;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class LoginEventIpAddressRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "login-event-ip-address-risk-evaluator";
    public static final String NAME = "Known IP Address risk evaluator";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new LoginEventIpAddressRiskEvaluator(session);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return LoginEventIpAddressRiskEvaluator.class;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String adminDisplayName() {
        return "Known IP address";
    }

    @Override
    public String adminEnabledHelpText() {
        return "Scores whether the current IP was seen in the user's successful login history.";
    }

    @Override
    public String adminTrustHelpText() {
        return "New or rare IPs increase risk; familiar IPs can reduce it.";
    }
}
