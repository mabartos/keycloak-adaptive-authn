package io.github.mabartos.engine.core;

import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class RiskEvaluationAuditPublisherTest {

  private static final SimpleRiskLevels STANDARD_BANDS = new SimpleRiskLevels(
      new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.33),
      new RiskLevel(SimpleRiskLevels.MEDIUM, 0.33, 0.66),
      new RiskLevel(SimpleRiskLevels.HIGH, 0.66, 1.0)
  );

  @Test
  void formatEvaluatorsJoinsValidScores() {
    var results = List.of(
        new AbstractRiskEngine.EvaluatorResult("ClientRoleRiskEvaluator", Risk.of(MEDIUM), 0.8, 12),
        new AbstractRiskEngine.EvaluatorResult("BrowserRiskEvaluator", Risk.of(NONE), 0.5, 3),
        new AbstractRiskEngine.EvaluatorResult("LoginFailuresRiskEvaluator", Risk.of(HIGH), 0.9, 45)
    );

    assertThat(
        RiskEvaluationAuditPublisher.formatEvaluators(results),
        is("LoginFailuresRiskEvaluator=HIGH, ClientRoleRiskEvaluator=MEDIUM, BrowserRiskEvaluator=NONE")
    );
  }

  @Test
  void formatEvaluatorsIncludesInvalidWithTruncatedReason() {
    var results = List.of(
        new AbstractRiskEngine.EvaluatorResult(
            "TimePatternRiskEvaluator",
            Risk.invalid("Building time pattern (login 0/4)"),
            1.0,
            0
        ),
        new AbstractRiskEngine.EvaluatorResult("KnownLocationRiskEvaluator", Risk.of(MEDIUM), 1.0, 1),
        new AbstractRiskEngine.EvaluatorResult(
            "AiAccountTakeoverEvaluator",
            Risk.invalid("No response from the Granite AI"),
            1.0,
            7
        )
    );

    assertThat(
        RiskEvaluationAuditPublisher.formatEvaluators(results),
        is("KnownLocationRiskEvaluator=MEDIUM, AiAccountTakeoverEvaluator=INVALID:No response from the Granite AI, TimePatternRiskEvaluator=INVALID:Building time pattern (login 0/4)")
    );
  }

  @Test
  void formatEvaluatorsSanitizesReasonSeparators() {
    var entry = RiskEvaluationAuditPublisher.formatEvaluatorEntry(
        new AbstractRiskEngine.EvaluatorResult(
            "Eval",
            Risk.invalid("a=b, c"),
            1.0,
            1
        )
    );
    assertThat(entry, is("Eval=INVALID:a b c"));
  }

  @Test
  void formatEvaluatorsCapsCount() {
    var results = new java.util.ArrayList<AbstractRiskEngine.EvaluatorResult>();
    for (int i = 0; i < 25; i++) {
      results.add(new AbstractRiskEngine.EvaluatorResult("Eval" + i, Risk.of(NONE), 1.0, 1));
    }

    var formatted = RiskEvaluationAuditPublisher.formatEvaluators(results);
    assertThat(formatted.split(", ").length, is(RiskEvaluationAuditPublisher.MAX_EVALUATORS_PER_PHASE));
  }

  @Test
  void formatScoreUsesFixedPrecision() {
    assertThat(RiskEvaluationAuditPublisher.formatScore(0.6123456), is("0.6123"));
  }

  @Test
  void resolveSimpleLevelNameUsesAlgorithmBands() {
    assertThat(RiskEvaluationAuditPublisher.resolveSimpleLevelName(0.154465, STANDARD_BANDS), is("LOW"));
    assertThat(RiskEvaluationAuditPublisher.resolveSimpleLevelName(0.55, STANDARD_BANDS), is("MEDIUM"));
    assertThat(RiskEvaluationAuditPublisher.resolveSimpleLevelName(0.9, STANDARD_BANDS), is("HIGH"));
  }
}
