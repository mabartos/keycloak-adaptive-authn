package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

public class AuthnSessionStoredRiskProvider implements StoredRiskProvider {
    private static final Logger logger = Logger.getLogger(AuthnSessionStoredRiskProvider.class);

    public static final String RISK_NO_USER_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_RISK_NO_USER";
    public static final String RISK_REQUIRES_USER_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_RISK_REQUIRES_USER";
    public static final String RISK_OVERALL_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_OVERALL_RISK";

    private final KeycloakSession session;

    public AuthnSessionStoredRiskProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Optional<Double> getStoredRisk() {
        return getStoredRisk(RiskPhase.OVERALL);
    }

    @Override
    public Optional<Double> getStoredRisk(RiskPhase riskPhase) {
        try {
            return Optional.ofNullable(session.getContext().getAuthenticationSession())
                    .map(f -> f.getAuthNote(getConfigProperty(riskPhase)))
                    .filter(StringUtil::isNotBlank)
                    .map(Double::parseDouble);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public void storeRisk(double risk) {
        storeRisk(risk, RiskPhase.OVERALL);
    }

    @Override
    public void storeRisk(double risk, RiskPhase riskPhase) {
        Optional.ofNullable(session.getContext().getAuthenticationSession())
                .ifPresentOrElse(f -> f.setAuthNote(getConfigProperty(riskPhase), Double.toString(risk)),
                        () -> {
                            throw new IllegalArgumentException("Authentication session is null");
                        });

        if (riskPhase != RiskPhase.OVERALL) { // Store Overall risk
            var oppositePhase = riskPhase == RiskPhase.NO_USER ? RiskPhase.REQUIRES_USER : RiskPhase.NO_USER;
            getStoredRisk(oppositePhase)
                    .ifPresent(oppositeRisk -> {
                        final var sum = risk + oppositeRisk;
                        final var result = sum / 2.0f;

                        logger.debugf("Stored overall risk: %f ('%s') + %f ('%s') = %f / 2.0 = %f", risk, riskPhase.name(), oppositeRisk, oppositePhase.name(), sum, result);

                        storeRisk(result, RiskPhase.OVERALL);
                    });
        }
    }

    static String getConfigProperty(RiskPhase riskPhase) {
        return switch (riskPhase) {
            case NO_USER -> RISK_NO_USER_AUTH_NOTE;
            case REQUIRES_USER -> RISK_REQUIRES_USER_AUTH_NOTE;
            case OVERALL -> RISK_OVERALL_AUTH_NOTE;
        };
    }

    @Override
    public void close() {

    }
}
