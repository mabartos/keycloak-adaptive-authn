package io.github.mabartos.evaluator;

import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class EvaluationPhaseAnnotationTest {

    @Test
    void allEvaluatorsMustHaveEvaluationPhase() throws Exception {
        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(
                        "META-INF/services/io.github.mabartos.spi.evaluator.RiskEvaluatorFactory")))) {

            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .forEach(className -> {
                        try {
                            var factory = (RiskEvaluatorFactory) Class.forName(className)
                                    .getDeclaredConstructor().newInstance();
                            var evaluatorClass = factory.evaluatorClass();
                            assertNotNull(
                                    evaluatorClass.getAnnotation(EvaluationPhase.class),
                                    "Missing @EvaluationPhase on " + evaluatorClass.getSimpleName()
                            );
                        } catch (ReflectiveOperationException e) {
                            fail("Cannot instantiate factory: " + className, e);
                        }
                    });
        }
    }
}
