package org.keycloak.adaptive.evaluator;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.context.browser.BrowserRiskEvaluatorFactory;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

public class LoginFailuresRiskEvaluator implements RiskEvaluator {
    private static final Logger logger = Logger.getLogger(LoginFailuresRiskEvaluator.class);

    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private Double risk;

    public LoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, LoginFailuresRiskEvaluatorFactory.NAME, Weight.IMPORTANT);
    }

    @Override
    public boolean requiresUser() {
        return true;
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

        // Num of failures
        var numFailures = loginFailures.getNumFailures();
        if (numFailures <= 2) {
            this.risk = Risk.NONE;
        } else if (numFailures <= 5) {
            this.risk = Risk.SMALL;
        } else if (numFailures < 10) {
            this.risk = Risk.MEDIUM;
        } else if (numFailures < 15) {
            this.risk = Risk.INTERMEDIATE;
        } else {
            this.risk = Risk.HIGH;
        }

        // Different IP address
        var currentIp = Optional.ofNullable(deviceContext.getData()).map(DeviceRepresentation::getIpAddress).orElse("");
        var lastIpFailure = loginFailures.getLastIPFailure();
        if (StringUtil.isBlank(currentIp) || StringUtil.isBlank(lastIpFailure)) {
            if (!currentIp.equals(lastIpFailure)) {
                this.risk = Math.max(risk, Risk.INTERMEDIATE);
                logger.debug("Request from different IP address");
            }
        }

        // TODO compute when was the last login failure
    }
}
