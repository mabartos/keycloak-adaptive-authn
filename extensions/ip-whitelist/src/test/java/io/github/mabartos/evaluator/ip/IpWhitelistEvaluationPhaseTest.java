package io.github.mabartos.evaluator.ip;

import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IpWhitelistEvaluationPhaseTest {

    @Test
    void factoryIsRegisteredWithBeforeAuthnPhase() {
        var factory = ServiceLoader.load(io.github.mabartos.spi.evaluator.RiskEvaluatorFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(f -> f instanceof IpWhitelistRiskEvaluatorFactory)
                .findFirst()
                .orElseThrow();

        var evaluatorClass = factory.evaluatorClass();
        var annotation = evaluatorClass.getAnnotation(EvaluationPhase.class);
        assertNotNull(annotation);
        assertEquals(BEFORE_AUTHN, annotation.value());
        assertEquals(BEFORE_AUTHN, factory.evaluationPhase());
    }
}
