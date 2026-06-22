package io.github.mabartos.ui;

import io.github.mabartos.evaluator.browser.BrowserRiskEvaluatorFactory;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ServiceLoader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskEvaluatorFactoryUiTest {

    static Stream<RiskEvaluatorFactory> registeredFactories() {
        return ServiceLoader.load(RiskEvaluatorFactory.class).stream()
                .map(ServiceLoader.Provider::get);
    }

    @ParameterizedTest
    @MethodSource("registeredFactories")
    void factoryDeclaresEvaluationPhase(RiskEvaluatorFactory factory) {
        assertNotNull(factory.evaluationPhase(),
                () -> factory.getClass().getName() + " must declare evaluationPhase()");
    }

    @ParameterizedTest
    @MethodSource("registeredFactories")
    void factoryProvidesAdminMetadata(RiskEvaluatorFactory factory) {
        assertFalse(factory.getName().isBlank());
        assertFalse(factory.getDescription().isBlank());
        assertFalse(RiskEvaluatorUi.enabledLabel(factory).isBlank());
        assertFalse(RiskEvaluatorUi.trustTooltip().isBlank());
    }

    @ParameterizedTest
    @MethodSource("registeredFactories")
    void getConfigProperties_usesDescription(RiskEvaluatorFactory factory) {
        factory.getConfigProperties().forEach(prop -> {
            assertFalse(prop.getHelpText().contains(".tooltip"),
                    () -> prop.getName() + " must not use unresolved i18n placeholder");
            assertFalse(prop.getHelpText().isBlank(), () -> prop.getName() + " help text must not be blank");
        });
    }

    @Test
    void getConfigProperties_enabledFieldUsesFactoryMetadata() {
        var factory = new BrowserRiskEvaluatorFactory();
        var enabled = factory.getConfigProperties().stream()
                .filter(p -> p.getName().startsWith("adaptive-evaluator-enabled-"))
                .findFirst()
                .orElseThrow();
        assertTrue(enabled.getHelpText().toLowerCase().contains("browser"));
        assertEquals("Browser", enabled.getLabel());
    }
}
