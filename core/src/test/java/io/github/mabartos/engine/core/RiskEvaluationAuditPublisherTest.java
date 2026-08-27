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
        new DefaultRiskEngine.EvaluatorResult("ClientRoleRiskEvaluator", Risk.of(MEDIUM), 0.8, 12, false),
        new DefaultRiskEngine.EvaluatorResult("BrowserRiskEvaluator", Risk.of(NONE), 0.5, 3, false),
        new DefaultRiskEngine.EvaluatorResult("LoginFailuresRiskEvaluator", Risk.of(HIGH), 0.9, 45, false)
    );

    assertThat(
        RiskEvaluationAuditPublisher.formatEvaluators(results),
        is("LoginFailuresRiskEvaluator=HIGH, ClientRoleRiskEvaluator=MEDIUM, BrowserRiskEvaluator=NONE")
    );
  }

  @Test
  void formatEvaluatorsIncludesInvalidWithTruncatedReason() {
    var results = List.of(
        new DefaultRiskEngine.EvaluatorResult(
            "TimePatternRiskEvaluator",
            Risk.invalid("Building time pattern (login 0/4)"),
            1.0,
            0,
            false
        ),
        new DefaultRiskEngine.EvaluatorResult("KnownLocationRiskEvaluator", Risk.of(MEDIUM), 1.0, 1, true),
        new DefaultRiskEngine.EvaluatorResult(
            "AiAccountTakeoverEvaluator",
            Risk.invalid("No response from the Granite AI"),
            1.0,
            7,
            true
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
        new DefaultRiskEngine.EvaluatorResult(
            "Eval",
            Risk.invalid("a=b, c"),
            1.0,
            1,
            false
        )
    );
    assertThat(entry, is("Eval=INVALID:a b c"));
  }

  @Test
  void formatEvaluatorsCapsCount() {
    var results = new java.util.ArrayList<DefaultRiskEngine.EvaluatorResult>();
    for (int i = 0; i < 25; i++) {
      results.add(new DefaultRiskEngine.EvaluatorResult("Eval" + i, Risk.of(NONE), 1.0, 1, false));
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
