package io.github.mabartos.docs;

import io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Generates {@code EVALUATORS.md} from all {@link RiskEvaluatorFactory} implementations
 * discovered via {@link ServiceLoader}.
 *
 * <p>Run with: {@code mvn -pl utils -am install -Dmaven.test.skip=true && mvn -pl utils exec:java@generate-evaluators-doc}
 */
public final class EvaluatorDocGenerator {

    private static final Map<EvaluationPhase, PhaseInfo> PHASE_INFO = new LinkedHashMap<>();

    static {
        PHASE_INFO.put(EvaluationPhase.BEFORE_AUTHN, new PhaseInfo(
                "Before Authentication",
                "Executed before the user is known. Useful for evaluating risk from browser, IP address, device, etc."
        ));
        PHASE_INFO.put(EvaluationPhase.USER_KNOWN, new PhaseInfo(
                "User Known",
                "Executed after identifying the user during authentication (e.g. after username + password). " +
                        "Useful for evaluating risk from user roles, login failures, login events, etc."
        ));
        PHASE_INFO.put(EvaluationPhase.CONTINUOUS, new PhaseInfo(
                "Continuous",
                "Re-evaluated at runtime when events occur and the risk score for the authenticated user should be recalculated. " +
                        "Should be used in conjunction with an event listener."
        ));
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: EvaluatorDocGenerator <project-root-dir>");
            System.exit(1);
        }

        Path outputFile = Path.of(args[0]).resolve("EVALUATORS.md");

        Map<EvaluationPhase, List<RiskEvaluatorFactory>> byPhase =
                RiskEvaluatorDocSupport.groupByPhase(RiskEvaluatorDocSupport.loadFactories());

        try (var out = new PrintWriter(outputFile.toFile(), StandardCharsets.UTF_8)) {
            out.println("# Risk Evaluators");
            out.println();
            out.println("> Auto-generated — do not edit manually.");
            out.println();

            for (EvaluationPhase phase : EvaluationPhase.values()) {
                PhaseInfo info = PHASE_INFO.get(phase);
                List<RiskEvaluatorFactory> factories = byPhase.getOrDefault(phase, List.of());

                out.printf("## %s (`%s`)%n", info.title(), phase.name());
                out.println();
                out.println(info.description());
                out.println();

                if (factories.isEmpty()) {
                    out.println("_No evaluators registered for this phase._");
                } else {
                    out.println("| Evaluator | Source | Description |");
                    out.println("|-----------|--------|-------------|");
                    for (RiskEvaluatorFactory f : factories) {
                        String source = RiskEvaluatorDocSupport.isExtension(f) ? "Extension" : "Core";
                        out.printf("| %s | %s | %s |%n",
                                escape(f.getName()), source, escape(f.getDescription()));
                    }
                }
                out.println();
            }

            out.println("---");
            out.println();
            out.println("Rows marked **Extension** come from optional modules under [extensions](extensions/) ");
            out.println("when present on the generator classpath (currently SSF). ");
            out.println("Backend-only modules such as `ip-api` (GeoIP) and `openrouter` (AI engine) do not register evaluators.");
            out.println();
            out.println("---");
            out.println();
            out.println("**Note:** This file is auto-generated. To regenerate it, run:");
            out.println();
            out.println("```bash");
            out.println("mvn -pl utils -am install -Dmaven.test.skip=true && mvn -pl utils exec:java@generate-evaluators-doc");
            out.println("```");
            out.println();
        }

        System.out.println("Generated " + outputFile.toAbsolutePath());
    }

    private static String escape(String text) {
        return text.replace("|", "\\|").replace("\n", " ");
    }

    private record PhaseInfo(String title, String description) {
    }
}
