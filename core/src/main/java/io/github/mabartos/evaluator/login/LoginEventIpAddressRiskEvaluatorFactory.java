package io.github.mabartos.evaluator.login;

import io.github.mabartos.evaluator.EvaluatorSettingProperties;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class LoginEventIpAddressRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "login-event-ip-address-risk-evaluator";
    public static final String NAME = "Known IP address";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new LoginEventIpAddressRiskEvaluator(session);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Scores whether the current IP was seen in the user's successful login history. New or rare IPs increase risk, familiar IPs can reduce it.";
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
    public List<ProviderConfigProperty> getAdditionalAdminConfigProperties() {
        var evaluatorClass = LoginEventIpAddressRiskEvaluator.class;
        return EvaluatorSettingProperties.of(
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, LoginEventIpAddressEvaluatorConfig.NEVER_SEEN_IP_SCORE_SETTING_KEY,
                        "Never seen IP score",
                        "Risk score when the current IP was never seen in the user's login history.",
                        LoginEventIpAddressEvaluatorConfig.DEFAULT_NEVER_SEEN_IP_SCORE),
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, LoginEventIpAddressEvaluatorConfig.FREQUENT_IP_SCORE_SETTING_KEY,
                        "Frequent IP score",
                        "Risk score when the current IP appears often in recent login history (trust signal).",
                        LoginEventIpAddressEvaluatorConfig.DEFAULT_FREQUENT_IP_SCORE),
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, LoginEventIpAddressEvaluatorConfig.OCCASIONAL_IP_SCORE_SETTING_KEY,
                        "Occasional IP score",
                        "Risk score when the current IP was seen before but not frequently enough.",
                        LoginEventIpAddressEvaluatorConfig.DEFAULT_OCCASIONAL_IP_SCORE),
                EvaluatorSettingProperties.intProperty(
                        evaluatorClass, LoginEventIpAddressEvaluatorConfig.MIN_LOGIN_EVENTS_SETTING_KEY,
                        "Min login events",
                        "Minimum successful login events in history before classifying an IP as frequent or occasional. "
                                + "Below this, the evaluator returns invalid.",
                        LoginEventIpAddressEvaluatorConfig.DEFAULT_MIN_LOGIN_EVENTS,
                        1),
                EvaluatorSettingProperties.intProperty(
                        evaluatorClass, LoginEventIpAddressEvaluatorConfig.FREQUENT_IP_THRESHOLD_DIVISOR_SETTING_KEY,
                        "Frequent IP threshold divisor",
                        "Current IP is frequent when its occurrence count is at least (event count / divisor). "
                                + "Default 3 means one third of recent logins.",
                        LoginEventIpAddressEvaluatorConfig.DEFAULT_FREQUENT_IP_THRESHOLD_DIVISOR,
                        1));
    }
}
