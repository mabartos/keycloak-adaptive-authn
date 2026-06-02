package io.github.mabartos.testsupport;

import io.github.mabartos.evaluator.client.ClientRoleRiskEvaluator;
import io.github.mabartos.evaluator.client.ClientSensitivityRiskEvaluator;
import io.github.mabartos.spi.evaluator.ContinuousRiskEvaluator;
import io.github.mabartos.spi.evaluator.DeviceRiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

/**
 * Resolves {@link RiskEvaluator#evaluationPhases()} for alignment checks against
 * {@link RiskEvaluatorFactory#evaluationPhase()} without {@code sun.misc.Unsafe}.
 */
public final class RuntimeEvaluationPhases {

    private RuntimeEvaluationPhases() {
    }

    public static Set<RiskEvaluator.EvaluationPhase> of(RiskEvaluatorFactory factory) {
        Class<? extends RiskEvaluator> evaluatorClass = factory.evaluatorClass();
        if (!declaresOwnEvaluationPhases(evaluatorClass)) {
            if (DeviceRiskEvaluator.class.isAssignableFrom(evaluatorClass)) {
                return Set.of(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
            }
            if (ContinuousRiskEvaluator.class.isAssignableFrom(evaluatorClass)) {
                return Set.of(RiskEvaluator.EvaluationPhase.CONTINUOUS);
            }
        }
        if (supportsNullSessionConstruction(evaluatorClass)) {
            return factory.create(null).evaluationPhases();
        }
        KeycloakSession session = EvaluatorConstructionKeycloakSession.create();
        try {
            return factory.create(session).evaluationPhases();
        } finally {
            session.close();
        }
    }

    private static boolean declaresOwnEvaluationPhases(Class<?> clazz) {
        try {
            return clazz.getDeclaredMethod("evaluationPhases").getDeclaringClass().equals(clazz);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean supportsNullSessionConstruction(Class<? extends RiskEvaluator> clazz) {
        return clazz == ClientSensitivityRiskEvaluator.class || clazz == ClientRoleRiskEvaluator.class;
    }
}
