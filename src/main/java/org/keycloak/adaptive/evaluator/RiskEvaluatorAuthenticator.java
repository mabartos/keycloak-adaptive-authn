package org.keycloak.adaptive.evaluator;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.engine.DefaultRiskEngineFactory;
import org.keycloak.adaptive.spi.engine.ConfigurableRequirements;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;

import static org.keycloak.adaptive.evaluator.RiskEvaluatorAuthenticatorFactory.REQUIRES_USER_CONFIG;

public class RiskEvaluatorAuthenticator implements Authenticator, ConfigurableRequirements {
    private static final Logger logger = Logger.getLogger(RiskEvaluatorAuthenticator.class);

    private final KeycloakSession session;
    private final StoredRiskProvider storedRiskProvider;
    private final RiskEngine riskEngine;

    public RiskEvaluatorAuthenticator(KeycloakSession session) {
        this.session = session;
        this.riskEngine = session.getProvider(RiskEngine.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var requiresUser = requiresUser(context.getAuthenticatorConfig());
        var riskPhase = requiresUser ? StoredRiskProvider.RiskPhase.REQUIRES_USER : StoredRiskProvider.RiskPhase.NO_USER;

        final var storedRisk = storedRiskProvider.getStoredRisk(riskPhase);

        if (storedRisk.isPresent()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", riskPhase, storedRisk.get());
        } else {
            riskEngine.evaluateRisk(requiresUser);
        }

        context.success();
    }

    @Override
    public boolean requiresUser(AuthenticatorConfigModel configModel) {
        return Optional.ofNullable(configModel)
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
