package org.keycloak.adaptive.evaluator;

import com.apicatalog.jsonld.StringUtils;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class EvaluatorUtils {

    private static Optional<String> getWeight(KeycloakSession session, String evaluatorName) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.getWeightConfig(evaluatorName)))
                .filter(StringUtils::isNotBlank);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, String evaluatorName, double defaultValue) {
        return getWeight(session, evaluatorName)
                .map(Double::parseDouble)
                .orElse(defaultValue);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, String evaluatorName) {
        return getStoredEvaluatorWeight(session, evaluatorName, Weight.NORMAL);
    }

    public static boolean existsStoredEvaluatorWeight(KeycloakSession session, String evaluatorName) {
        return getWeight(session, evaluatorName).isPresent();
    }

    public static void storeEvaluatorWeight(KeycloakSession session, String evaluatorName, double value) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.getWeightConfig(evaluatorName), Double.toString(value)));
    }
}
