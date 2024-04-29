package org.keycloak.adaptive.evaluator;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.context.browser.BrowserRiskEvaluatorFactory;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.ai.AiEngine;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;

public class AiLoginFailuresRiskEvaluator implements RiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiLoginFailuresRiskEvaluator.class);

    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final AiEngine aiEngine;
    private Double risk;

    public AiLoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiEngine.class);
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, AiLoginFailuresRiskEvaluatorFactory.class, Weight.IMPORTANT);
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, AiLoginFailuresRiskEvaluatorFactory.class);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    protected static String request(UserLoginFailureModel loginFailures, DeviceRepresentation device) {
        var currentIp = Optional.ofNullable(device).map(DeviceRepresentation::getIpAddress);

        // we should be careful about the message poisoning
        var request = String.format("""
                        Give me the overall risk that the user trying to authenticate is fraud based on its parameters.
                        These parameters shows the metrics about login failures for the particular user.
                        Used for detection of the brute force attacks.
                        After each successful login, these metrics are reset.
                        -----
                        Number of login failures for the user: %d
                        IP address of the last login failure: %s
                        Current device IP address: %s
                        Number of temporary lockouts for the user: %d
                        Last failure was before: %d ms
                        -----
                        """,
                loginFailures.getNumFailures(),
                loginFailures.getLastIPFailure(),
                currentIp.orElse("unknown"), // TODO use IP Address context
                loginFailures.getNumTemporaryLockouts(),
                Time.currentTimeMillis() - loginFailures.getLastFailure()
        );

        logger.debugf("AI login failures request: %s", request);
        return request;
    }

    @Override
    public void evaluate() {
        var realm = session.getContext().getRealm();
        if (realm == null) {
            logger.debug("Context realm is null");
            return;
        }

        var user = Optional.ofNullable(session.getContext().getAuthenticationSession())
                .map(AuthenticationSessionModel::getAuthenticatedUser);
        if (user.isEmpty()) {
            logger.debug("Context user is null");
            return;
        }

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, user.get().getId());
        if (loginFailures == null) {
            logger.debug("Cannot obtain login failures");
            return;
        }

        Optional<Double> evaluatedRisk = EvaluatorUtils.getRiskFromAi(aiEngine, request(loginFailures, deviceContext.getData()));
        evaluatedRisk.ifPresent(risk -> {
            logger.debugf("AI request was successful. Evaluated risk: %f", risk);
            this.risk = risk;
        });
    }
}
