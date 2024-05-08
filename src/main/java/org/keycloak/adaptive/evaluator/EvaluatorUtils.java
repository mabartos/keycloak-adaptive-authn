package org.keycloak.adaptive.evaluator;

import com.apicatalog.jsonld.StringUtils;
import org.keycloak.adaptive.ai.OpenAiDataResponse;
import org.keycloak.adaptive.ai.OpenAiEngine;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.adaptive.spi.ai.AiRiskEvaluatorMessages;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

public class EvaluatorUtils {

    public static Optional<Double> getRiskFromAi(AiNlpEngine aiEngine, String message) {
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

    private static Optional<String> getWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.getWeightConfig(evaluatorFactory)))
                .filter(StringUtils::isNotBlank);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, double defaultValue) {
        return getWeight(session, evaluatorFactory)
                .map(Double::parseDouble)
                .orElse(defaultValue);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return getStoredEvaluatorWeight(session, evaluatorFactory, Weight.NORMAL);
    }

    public static boolean existsStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return getWeight(session, evaluatorFactory).isPresent();
    }

    public static void storeEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, double value) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.getWeightConfig(evaluatorFactory), Double.toString(value)));
    }

    public static boolean isEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, boolean defaultValue) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.isEnabledConfig(evaluatorFactory)))
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public static boolean isEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return isEvaluatorEnabled(session, evaluatorFactory, true);
    }

    public static void setEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, boolean enabled) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.isEnabledConfig(evaluatorFactory), Boolean.valueOf(enabled).toString()));
    }
}
