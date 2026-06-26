/**
 * Browser-side simulation of LogOddsRiskAlgorithm.
 *
 * Intentionally mirrors (does not call) the Java implementation:
 * - Per phase: same as evaluateRisk() — sum(evidence * trust) + bias, then logistic.
 * - Overall: same as getOverallRisk() — totalEvidence(BEFORE_AUTHN) + totalEvidence(USER_KNOWN), then logistic.
 *   CONTINUOUS is shown for exploration only and is excluded from the global total.
 * - Level bands: same rule as RiskLevel.matchesRisk().
 *
 * Score → evidence mapping and thresholds are injected from Java at HTML generation time;
 * this file only implements the aggregation math. Keep in sync when changing LogOddsRiskAlgorithm.
 */
function logistic(x) {
  return 1 / (1 + Math.exp(-x));
}

function fmtProb(p) {
  return p.toFixed(3);
}

function fmtNum(n, digits = 2) {
  return n.toFixed(digits);
}

function evidenceClass(v) {
  if (v > 0) return "evidence-pos";
  if (v < 0) return "evidence-neg";
  return "evidence-zero";
}

function readLevels(tableId) {
  return [...document.querySelectorAll("#" + tableId + " tr[data-level]")].map((row) => ({
    name: row.dataset.level,
    min: parseFloat(row.dataset.min),
    max: parseFloat(row.dataset.max),
  }));
}

function matchesRiskLevel(p, level) {
  if (level.min === 0.0 && p === 0.0) return true;
  return p > level.min && p <= level.max;
}

function levelFromProbLevels(p, levels) {
  const found = levels.find((l) => matchesRiskLevel(p, l));
  return found ? found.name : "-";
}

function levelBadgeClass(name) {
  if (name === "HIGH") return "high";
  if (name === "MEDIUM" || name === "MODERATE" || name === "MILD") return "medium";
  return "low";
}

function selectedEvidence(select) {
  const option = select.selectedOptions[0];
  return option ? parseFloat(option.dataset.evidence) : NaN;
}

function calcPhase(tbody, bias, includeBias = true) {
  let sum = 0;
  tbody.querySelectorAll("tr.eval-row").forEach((tr) => {
    const active = tr.querySelector(".active").checked;
    const scoreSelect = tr.querySelector(".score");
    const trust = parseFloat(tr.querySelector(".trust").value);
    const evidence = selectedEvidence(scoreSelect);
    const evCell = tr.querySelector(".evidence");
    const coCell = tr.querySelector(".contribution");

    if (!active || isNaN(evidence) || isNaN(trust) || trust < 0 || trust > 1) {
      evCell.textContent = "-";
      evCell.className = "num evidence evidence-zero";
      coCell.textContent = "-";
      coCell.className = "num contribution";
      return;
    }

    const contribution = evidence * trust;
    sum += contribution;
    evCell.textContent = fmtNum(evidence);
    evCell.className = "num evidence " + evidenceClass(evidence);
    coCell.textContent = fmtNum(contribution);
    coCell.className = "num contribution " + evidenceClass(contribution);
  });

  const totalEvidence = includeBias ? sum + bias : sum;
  return { sum, totalEvidence, prob: logistic(totalEvidence) };
}

function renderTotals(container, result) {
  container.innerHTML = ""
    + '<div class="metric"><div class="label">Σ evidence</div><div class="value ' + evidenceClass(result.sum) + '">' + fmtNum(result.sum) + "</div></div>"
    + '<div class="metric"><div class="label">totalEvidence (+ bias)</div><div class="value">' + fmtNum(result.totalEvidence) + "</div></div>"
    + '<div class="metric"><div class="label">Phase P(fraud)</div><div class="value">' + fmtProb(result.prob) + "</div></div>";
}

function recalc() {
  const simpleLevels = readLevels("ref-simple");
  const advancedLevels = readLevels("ref-advanced");
  const bias = parseFloat(document.getElementById("bias").value);
  const biasPrior = document.getElementById("bias-prior");
  if (isNaN(bias)) {
    biasPrior.textContent = "-";
  } else {
    biasPrior.textContent = fmtProb(logistic(bias));
  }

  const b = isNaN(bias) ? 0 : bias;
  const before = calcPhase(document.querySelector('[data-phase="before"]'), b);
  const user = calcPhase(document.querySelector('[data-phase="user"]'), b);
  renderTotals(document.querySelector('[data-totals="before"]'), before);
  renderTotals(document.querySelector('[data-totals="user"]'), user);

  const continuousBody = document.querySelector('[data-phase="continuous"]');
  if (continuousBody) {
    renderTotals(document.querySelector('[data-totals="continuous"]'), calcPhase(continuousBody, b));
  }

  const globalEvidence = before.totalEvidence + user.totalEvidence;
  const globalProb = logistic(globalEvidence);
  const simpleLabel = levelFromProbLevels(globalProb, simpleLevels);

  document.getElementById("global-evidence").textContent = fmtNum(globalEvidence);
  document.getElementById("global-evidence").className = "value " + evidenceClass(globalEvidence);
  document.getElementById("global-prob").textContent = fmtProb(globalProb);

  const badge = document.getElementById("global-level");
  badge.textContent = simpleLabel;
  badge.className = "badge " + levelBadgeClass(simpleLabel);

  const advLabel = levelFromProbLevels(globalProb, advancedLevels);
  const advBadge = document.getElementById("global-level-advanced");
  advBadge.textContent = advLabel;
  advBadge.className = "badge " + levelBadgeClass(advLabel);
}

function wireRow(tr) {
  tr.querySelector(".active").addEventListener("change", (e) => {
    tr.classList.toggle("inactive", !e.target.checked);
    recalc();
  });
  tr.querySelectorAll("input.trust, select.score").forEach((el) => {
    el.addEventListener("input", recalc);
    el.addEventListener("change", recalc);
  });
}

document.getElementById("bias").addEventListener("input", recalc);
document.querySelectorAll("tr.eval-row").forEach(wireRow);
recalc();
