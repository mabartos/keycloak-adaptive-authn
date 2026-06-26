package io.github.mabartos.docs;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Generates {@code utils/log-odds-calculator/log-odds-calculator.html} from
 * {@code calculator.ftl} and core SPI / algorithm constants.
 *
 * <p>Run with: {@code mvn -pl core compile exec:java@algorithm-calculator}
 */
public final class LogOddsCalculatorGenerator {

    private static final String REGENERATE_COMMAND = "mvn -pl core compile exec:java@algorithm-calculator";
    private static final String DEFAULT_SCORE = "NONE";
    private static final double DEFAULT_TRUST = 1.0;

    private LogOddsCalculatorGenerator() {
    }

    public static void main(String[] args) throws IOException, TemplateException {
        if (args.length < 1) {
            System.err.println("Usage: LogOddsCalculatorGenerator <project-root-dir>");
            System.exit(1);
        }

        Path calculatorDir = Path.of(args[0]).resolve("utils/log-odds-calculator");
        generate(calculatorDir, calculatorDir.resolve("log-odds-calculator.html"));
        System.out.println("Generated " + calculatorDir.resolve("log-odds-calculator.html").toAbsolutePath());
    }

    static void generate(Path templateDir, Path outputFile) throws IOException, TemplateException {
        Files.createDirectories(outputFile.getParent());

        Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setDirectoryForTemplateLoading(templateDir.toFile());
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);

        Template template = configuration.getTemplate("calculator.ftl");
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            template.process(buildModel(), writer);
        }
    }

    static Map<String, Object> buildModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("regenerateCommand", REGENERATE_COMMAND);
        model.put("defaultBias", LogOddsAlgorithmConstants.defaultBias());
        model.put("defaultScore", DEFAULT_SCORE);
        model.put("defaultTrust", DEFAULT_TRUST);
        model.put("scoreEvidence", toScoreRows());
        model.put("simpleLevels", toLevelRows(LogOddsAlgorithmConstants.simpleLevels()));
        model.put("advancedLevels", toLevelRows(LogOddsAlgorithmConstants.advancedLevels()));
        model.put("phaseSections", phaseSections());
        model.put("evaluatorsByPhase", evaluatorsByPhase());
        return model;
    }

    private static List<Map<String, String>> phaseSections() {
        return List.of(
                phaseSection("before", "Phase BEFORE_AUTHN",
                        "Evaluators run before user identification (device, client, GeoIP bootstrap, etc.).", false),
                phaseSection("user", "Phase USER_KNOWN",
                        "Evaluators run after the user is identified during authentication.", false),
                phaseSection("continuous", "Phase CONTINUOUS",
                        "Runtime re-evaluation when events occur. Not included in login overall risk "
                                + "(LogOddsRiskAlgorithm.getOverallRisk sums BEFORE_AUTHN + USER_KNOWN only).", true)
        );
    }

    private static Map<String, String> phaseSection(String phaseKey, String title, String description, boolean continuous) {
        Map<String, String> section = new LinkedHashMap<>();
        section.put("phaseKey", phaseKey);
        section.put("title", title);
        section.put("description", description);
        section.put("continuous", String.valueOf(continuous));
        return section;
    }

    private static Map<String, List<Map<String, String>>> evaluatorsByPhase() {
        Map<EvaluationPhase, List<RiskEvaluatorFactory>> byPhase = StreamSupport
                .stream(ServiceLoader.load(RiskEvaluatorFactory.class).spliterator(), false)
                .sorted(Comparator.comparing(RiskEvaluatorFactory::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(
                        RiskEvaluatorFactory::evaluationPhase,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        result.put("before", toEvaluatorRows(byPhase.getOrDefault(EvaluationPhase.BEFORE_AUTHN, List.of())));
        result.put("user", toEvaluatorRows(byPhase.getOrDefault(EvaluationPhase.USER_KNOWN, List.of())));
        result.put("continuous", toEvaluatorRows(byPhase.getOrDefault(EvaluationPhase.CONTINUOUS, List.of())));
        return result;
    }

    private static List<Map<String, String>> toEvaluatorRows(List<RiskEvaluatorFactory> factories) {
        return factories.stream().map(factory -> {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("id", factory.getId());
            row.put("name", factory.getName());
            row.put("description", factory.getDescription());
            return row;
        }).toList();
    }

    private static List<Map<String, String>> toScoreRows() {
        return LogOddsAlgorithmConstants.scoreEvidenceMapping().stream().map(row -> {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("name", row.score());
            map.put("evidence", formatNumber(row.evidence()));
            map.put("label", row.label());
            return map;
        }).toList();
    }

    private static List<Map<String, String>> toLevelRows(List<LogOddsAlgorithmConstants.LevelThreshold> levels) {
        return levels.stream().map(level -> {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("name", level.name());
            map.put("min", formatNumber(level.min()));
            map.put("max", formatNumber(level.max()));
            map.put("range", formatLevelRange(level.min(), level.max()));
            return map;
        }).toList();
    }

    private static String formatLevelRange(double min, double max) {
        if (min == 0.0) {
            return String.format(Locale.US, "0 – %.2f", max);
        }
        return String.format(Locale.US, "> %.2f – %.2f", min, max);
    }

    private static String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
