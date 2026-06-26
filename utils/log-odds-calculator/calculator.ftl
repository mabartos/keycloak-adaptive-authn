<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Log-Odds Calculator - Keycloak Adaptive Authn</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@patternfly/patternfly@5.4.2/patternfly.min.css">
  <link rel="stylesheet" href="calculator.css">
</head>
<body>
  <div class="kc-page">
    <header class="kc-masthead">
      <div class="kc-masthead-brand">
        <img
          src="../../docs/img/adaptive-authentication-logo.png"
          alt="Keycloak Adaptive Authentication"
          class="kc-logo"
          width="420"
          height="48"
        >
      </div>
    </header>

    <main class="kc-main">
      <div class="kc-banner">
        Auto-generated - do not edit manually. Regenerate: <code>${regenerateCommand}</code>
      </div>

      <div class="kc-page-header">
        <h1>Log-Odds Calculator</h1>
        <p>Simulates <code>LogOddsRiskAlgorithm</code> using score mapping and thresholds from this project.</p>
      </div>

      <section>
        <h2>Prior (bias)</h2>
        <div class="bias-row">
          <div>
            <label for="bias">Log-odds bias (per phase)</label>
            <input type="number" id="bias" step="0.1" value="${defaultBias?c}">
          </div>
          <div class="metric">
            <div class="label">P(fraud) with zero evaluator evidence (one phase)</div>
            <div class="value" id="bias-prior">-</div>
          </div>
        </div>
      </section>

      <#list phaseSections as section>
      <section id="phase-${section.phaseKey}">
        <h2>${section.title}</h2>
        <p class="phase-note<#if section.continuous == "true"> phase-continuous</#if>">${section.description}</p>
        <table>
          <thead>
            <tr>
              <th>Evaluator</th>
              <th>Active</th>
              <th>Risk.Score</th>
              <th class="num">Evidence</th>
              <th class="num">Trust</th>
              <th class="num">Contribution</th>
            </tr>
          </thead>
          <tbody data-phase="${section.phaseKey}">
            <#list evaluatorsByPhase[section.phaseKey] as ev>
            <tr class="eval-row">
              <td>
                <div class="eval-name">${ev.name?html}</div>
                <div class="eval-hint">${ev.description?html}</div>
                <div class="eval-id">${ev.id?html}</div>
              </td>
              <td><input type="checkbox" class="active" checked title="Unchecked = Risk.INVALID (excluded)"></td>
              <td>
                <select class="score">
                  <#list scoreEvidence as score>
                  <option value="${score.name}" data-evidence="${score.evidence}"<#if score.name == defaultScore> selected</#if>>${score.name}</option>
                  </#list>
                </select>
              </td>
              <td class="num evidence">-</td>
              <td class="num"><input type="number" class="trust" min="0" max="1" step="0.1" value="${defaultTrust?c}" style="width:4rem"></td>
              <td class="num contribution">-</td>
            </tr>
            </#list>
          </tbody>
        </table>
        <div class="totals" data-totals="${section.phaseKey}"></div>
      </section>
      </#list>

      <section class="global">
        <h2>Overall risk (after USER_KNOWN)</h2>
        <p class="phase-note" style="border-left-color:var(--kc-primary);margin-top:-0.25rem">
          global totalEvidence = totalEvidence<sub>BEFORE</sub> + totalEvidence<sub>USER</sub>
          (bias is counted twice when both phases have active rows; CONTINUOUS is excluded)
        </p>
        <div class="totals">
          <div class="metric">
            <div class="label">Global totalEvidence</div>
            <div class="value" id="global-evidence">-</div>
          </div>
          <div class="metric">
            <div class="label">Global P(fraud)</div>
            <div class="big" id="global-prob">-</div>
          </div>
          <div class="metric">
            <div class="label">Simple level (3)</div>
            <div><span class="badge" id="global-level">-</span></div>
          </div>
          <div class="metric">
            <div class="label">Advanced level (5)</div>
            <div><span class="badge" id="global-level-advanced">-</span></div>
          </div>
        </div>
      </section>

      <section>
        <h2>Reference</h2>
        <div class="ref-grid">
          <div class="ref-block">
            <h3>Risk.Score → evidence</h3>
            <table class="ref-table">
              <thead><tr><th>Score</th><th class="num">Evidence</th><th>Meaning</th></tr></thead>
              <tbody>
                <#list scoreEvidence as score>
                <tr><td>${score.name}</td><td class="num">${score.evidence}</td><td>${score.label?html}</td></tr>
                </#list>
              </tbody>
            </table>
          </div>
          <div class="ref-block">
            <h3>Final P(fraud) → simple (3 levels)</h3>
            <table class="ref-table" id="ref-simple">
              <thead><tr><th>Level</th><th class="num">P(fraud) range</th></tr></thead>
              <tbody>
                <#list simpleLevels as level>
                <tr data-level="${level.name}" data-min="${level.min}" data-max="${level.max}">
                  <td>${level.name}</td><td class="num">${level.range}</td>
                </tr>
                </#list>
              </tbody>
            </table>
          </div>
          <div class="ref-block">
            <h3>Final P(fraud) → advanced (5 levels)</h3>
            <table class="ref-table" id="ref-advanced">
              <thead><tr><th>Level</th><th class="num">P(fraud) range</th></tr></thead>
              <tbody>
                <#list advancedLevels as level>
                <tr data-level="${level.name}" data-min="${level.min}" data-max="${level.max}">
                  <td>${level.name}</td><td class="num">${level.range}</td>
                </tr>
                </#list>
              </tbody>
            </table>
          </div>
        </div>
        <p class="ref-note">
          Default level thresholds from <code>LogOddsDefaultRiskLevels</code> (used by <code>LogOddsRiskAlgorithmFactory</code>).
          Range rule matches <code>RiskLevel.matchesRisk()</code>: 0 is LOW; otherwise (min, max].
          Evaluator list is loaded from <code>RiskEvaluatorFactory</code> via ServiceLoader (core module only).
        </p>
      </section>
    </main>

    <footer class="kc-footer">
      Additional evaluators are available in the
      <a href="../../extensions/">extensions</a> directory.
    </footer>
  </div>

  <script src="calculator.js"></script>
</body>
</html>
