package org.keycloak.adaptive.evaluator;

import com.apicatalog.jsonld.StringUtils;
import org.keycloak.adaptive.ai.OpenAiDataResponse;
import org.keycloak.adaptive.ai.OpenAiEngine;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.ai.AiEngine;
import org.keycloak.adaptive.spi.ai.AiRiskEvaluatorMessages;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

public class EvaluatorUtils {


    public static Optional<Double> getRiskFromAi(AiEngine aiEngine, String message) {
        if (aiEngine instanceof OpenAiEngine) {
            OpenAiDataResponse response = aiEngine.getResult(AiRiskEvaluatorMessages.CONTEXT_MESSAGE, message, OpenAiDataResponse.class);

            return Optional.ofNullable(response)
                    .flatMap(f -> f.choices().stream().findAny())
                    .map(OpenAiDataResponse.Choice::message)
                    .map(OpenAiDataResponse.Choice.Message::content)
                    .filter(StringUtil::isNotBlank)
                    .map(Double::parseDouble);
        }
        return Optional.empty();
    }

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
