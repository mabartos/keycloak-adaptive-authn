package io.github.mabartos.evaluator;

import org.jboss.logging.Logger;
import io.github.mabartos.spi.engine.ConfigurableRequirements;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;

import static io.github.mabartos.evaluator.RiskEvaluatorAuthenticatorFactory.REQUIRES_USER_CONFIG;

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
        var phase = requiresUser ? RiskEvaluator.EvaluationPhase.USER_KNOWN : RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;

        final var storedRisk = storedRiskProvider.getStoredRisk(phase);

        if (storedRisk.isValid()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", phase.name(), storedRisk.getScore());
        } else {
            riskEngine.evaluateRisk(phase, context.getRealm(), context.getUser());
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
