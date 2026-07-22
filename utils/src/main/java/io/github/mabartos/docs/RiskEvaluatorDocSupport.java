package io.github.mabartos.docs;

import io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;

import java.security.CodeSource;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Shared discovery helpers for documentation generators. */
final class RiskEvaluatorDocSupport {

    private RiskEvaluatorDocSupport() {
    }

    static List<RiskEvaluatorFactory> loadFactories() {
        return StreamSupport.stream(ServiceLoader.load(RiskEvaluatorFactory.class).spliterator(), false)
                .sorted(Comparator
                        .comparing(RiskEvaluatorDocSupport::isExtension)
                        .thenComparing(RiskEvaluatorFactory::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static Map<EvaluationPhase, List<RiskEvaluatorFactory>> groupByPhase(List<RiskEvaluatorFactory> factories) {
        return factories.stream().collect(Collectors.groupingBy(
                RiskEvaluatorFactory::evaluationPhase,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    /**
     * True when the factory was loaded from an {@code extensions/} module jar
     * ({@code keycloak-adaptive-ext-*}), not from core.
     */
    static boolean isExtension(RiskEvaluatorFactory factory) {
        try {
            CodeSource codeSource = factory.getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return false;
            }
            String path = codeSource.getLocation().getPath();
            return path.contains("keycloak-adaptive-ext-") || path.contains("/extensions/");
        } catch (SecurityException e) {
            return false;
        }
    }
}
