package io.github.mabartos;

import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies risk evaluator factories registered in the embedded Keycloak server expose a
 * consistent evaluation phase for admin UI grouping ({@link RiskEvaluatorFactory#evaluationPhase()}).
 */
@KeycloakIntegrationTest(config = RiskEvaluatorFactoryPhaseTest.Config.class)
class RiskEvaluatorFactoryPhaseTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void registeredFactoriesExposeEvaluationPhaseFromAnnotation() {
        runOnServer.run(session -> {
            var factories = session.getKeycloakSessionFactory()
                    .getProviderFactoriesStream(RiskEvaluator.class)
                    .map(RiskEvaluatorFactory.class::cast)
                    .toList();

            assertTrue(factories.size() > 0, "Expected at least one RiskEvaluatorFactory on the server");

            factories.forEach(factory -> {
                var evaluatorClass = factory.evaluatorClass();
                var annotation = evaluatorClass.getAnnotation(EvaluationPhase.class);
                assertNotNull(annotation,
                        () -> "Missing @EvaluationPhase on " + evaluatorClass.getSimpleName()
                                + " (factory " + factory.getClass().getName() + ")");
                assertEquals(annotation.value(), factory.evaluationPhase(),
                        () -> factory.getClass().getName() + " evaluationPhase() must match @EvaluationPhase on "
                                + evaluatorClass.getSimpleName());
            });
        });
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("features", "declarative-ui");
        }
    }
}
