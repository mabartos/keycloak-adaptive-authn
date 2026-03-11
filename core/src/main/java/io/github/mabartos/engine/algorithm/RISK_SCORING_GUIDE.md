# Risk Scoring Guide for Log-Odds Algorithm

This guide provides a reference for setting `Risk.Score` values in evaluators when using the **LogOddsRiskAlgorithm**. Unlike the weighted average algorithm, the log-odds model uses evidence scores that can be positive (risk) or negative (trust).

## Evidence Score Mapping

| Risk.Score | Evidence Value | Probability Impact | Use Case |
|-----------|----------------|-------------------|----------|
| **NEGATIVE_HIGH** | -2.5 | Strong trust signal | Known good device, whitelisted IP, verified trusted location |
| **NEGATIVE_LOW** | -0.3 | Weak trust signal | Recognized device, familiar location, normal behavior |
| **NONE** | 0.0 | Neutral | No evidence either way, baseline state |
| **VERY_SMALL** | +0.1 | Minimal risk | Slight deviation from normal, first-time occurrence |
| **SMALL** | +0.4 | Low risk | Minor anomaly, acceptable deviation |
| **MEDIUM** | +0.8 | Moderate risk | Notable anomaly, warrants attention |
| **HIGH** | +1.5 | High risk | Strong anomaly, suspicious signal |
| **VERY_HIGH** | +2.0 | Very high risk | Severe anomaly, likely attack indicator |
| **EXTREME** | +2.5 | Extreme risk | Known attack vector, definite threat |

## Evidence Examples by Signal Type

### Device & Browser Signals

| Signal | Risk.Score | Rationale |
|--------|-----------|-----------|
| Known device (seen 10+ times) | NEGATIVE_LOW | Trust signal - familiar device |
| Device seen 2-5 times | NONE | Not enough data to trust or distrust |
| New device (first time) | SMALL to MEDIUM | Depends on context - first login vs. established user |
| Unknown browser/OS | MEDIUM to HIGH | Moderate anomaly |
| Device fingerprint mismatch | HIGH | Strong anomaly - possible spoofing |

### Location Signals

| Signal | Risk.Score | Rationale |
|--------|-----------|-----------|
| Exact location match (same city) | NEGATIVE_LOW | Trust signal - normal location |
| Same country, different city | NONE to VERY_SMALL | Minor change, often legitimate |
| New country (plausible travel) | SMALL to MEDIUM | Could be travel or VPN |
| New country + impossible travel | HIGH to VERY_HIGH | Physical impossibility suggests attack |
| Known Tor exit node | EXTREME | Anonymization = strong fraud signal |
| Known VPN/proxy | HIGH | Anonymization attempt |

### Temporal Signals

| Signal | Risk.Score | Rationale |
|--------|-----------|-----------|
| Login at typical time (±2h) | NEGATIVE_LOW | Trust signal - normal pattern |
| Slight time deviation (2-4h) | VERY_SMALL to SMALL | Minor anomaly |
| Moderate deviation (4-6h) | MEDIUM | Notable anomaly |
| Large deviation (6-8h) | HIGH | Very unusual timing |
| Extreme deviation (>8h + midnight hours) | VERY_HIGH to EXTREME | Opposite time of day = strong signal |

### Authentication Failure Signals

| Signal | Risk.Score | Rationale |
|--------|-----------|-----------|
| 0-2 recent failures | NONE | Normal - users make mistakes |
| 3-5 failures | SMALL | Possible forgotten password |
| 6-9 failures | MEDIUM | Suspicious pattern |
| 10-14 failures | HIGH | Likely brute force |
| 15+ failures | VERY_HIGH | Definite attack |
| Distributed attack (multiple IPs) | VERY_HIGH to EXTREME | Coordinated attack |

### Behavioral Signals

| Signal | Risk.Score | Rationale |
|--------|-----------|-----------|
| Normal user actions | NONE | Expected behavior |
| Admin role login | SMALL to MEDIUM | Higher value target |
| 1-2 sensitive actions | VERY_SMALL | Normal account management |
| 3-5 sensitive actions (short time) | MEDIUM | Unusual activity burst |
| 5-10 sensitive actions | HIGH | Suspicious activity |
| 10+ sensitive actions | VERY_HIGH | Likely account takeover |
| Sequence: change email → password → remove MFA | EXTREME | Classic takeover pattern |

## Evaluator Design Patterns

### Pattern 1: Binary Signal (Known/Unknown)
```java
// Device is either known or unknown
return knownDevice ? Risk.of(NEGATIVE_LOW) : Risk.of(MEDIUM);
```

### Pattern 2: Graduated Scale
```java
// Map continuous values to discrete risk levels
if (failureCount <= 2) return Risk.of(NONE);
else if (failureCount <= 5) return Risk.of(SMALL);
else if (failureCount < 10) return Risk.of(MEDIUM);
else if (failureCount < 15) return Risk.of(HIGH);
else return Risk.of(VERY_HIGH);
```

### Pattern 3: Context-Dependent
```java
// Same signal has different meanings in different contexts
if (newCountry && impossibleTravel) {
    return Risk.of(VERY_HIGH);  // Can't physically travel that fast
} else if (newCountry) {
    return Risk.of(SMALL);  // Could be legitimate travel
}
```

### Pattern 4: Composite Max
```java
// Multiple sub-signals, take maximum
Risk risk = Risk.of(NONE);
risk = risk.max(evaluateFailureCount());
risk = risk.max(evaluateFailureTiming());
risk = risk.max(evaluateIpPattern());
return risk;
```

## Guidelines for Setting Risk Levels

1. **Use trust signals (NEGATIVE_*) sparingly**
   - Only when you have strong positive evidence (known good state)
   - Don't use negative scores for "absence of bad" - use NONE instead

2. **Reserve EXTREME for definite threats**
   - Known malicious IPs (Tor, blacklists)
   - Impossible scenarios (impossible travel)
   - Confirmed attack patterns

3. **Use MEDIUM as the default anomaly level**
   - When something is unusual but not necessarily malicious
   - First occurrence of a signal

4. **Consider the evaluator's weight**
   - High weight evaluators can use more conservative scores
   - Low weight evaluators should be more aggressive with HIGH/VERY_HIGH

5. **Think in combinations**
   - Individual signals might be SMALL or MEDIUM
   - The log-odds algorithm will naturally combine them
   - Example: MEDIUM (0.8) + SMALL (0.4) + MEDIUM (0.8) = 2.0 evidence → 88% risk

## Evidence Score Calculation Examples

### Single Signal
- User logs in from new device (MEDIUM = +0.8)
- logistic(0.8) = 69% risk probability

### Two Signals
- New device (MEDIUM = +0.8) + Unusual time (SMALL = +0.4)
- logistic(1.2) = 77% risk probability

### Multiple Signals
- Tor IP (EXTREME = +2.5) + New device (MEDIUM = +0.8) + Failed logins (SMALL = +0.4)
- logistic(3.7) = 98% risk probability

### Trust Signal Offsetting Risk
- New device (MEDIUM = +0.8) + Known location (NEGATIVE_LOW = -0.3)
- logistic(0.5) = 62% risk probability

## Testing Your Risk Levels

When implementing or adjusting risk levels, ask:

1. **What's the base rate?** How often does this signal occur legitimately?
2. **How predictive is it?** Does this signal actually correlate with fraud?
3. **What's the cost of false positive?** Blocking legitimate users vs. letting fraud through
4. **Combined effect?** Will this work well with other evaluators?

## Version History
- Initial version: 2026-03-11
