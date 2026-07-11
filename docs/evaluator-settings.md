# Evaluator settings

Risk evaluators can expose realm-level settings beyond **enabled** and **trust**. These are stored as realm attributes and edited in **Authentication → Risk-based policies** (requires `declarative-ui`).

## Naming convention

```
adaptive-evaluator-{settingKey}-{EvaluatorSimpleClassName}
```

Built with `RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey)`.

Example for Known location:

| Realm attribute | Default | Effect |
|-----------------|---------|--------|
| `adaptive-evaluator-ttl-days-KnownLocationRiskEvaluator` | `90` | Days before a known location stops providing a trust signal |
| `adaptive-evaluator-max-stored-locations-KnownLocationRiskEvaluator` | `10` | Maximum known locations kept per user (minimum `1`) |
| `adaptive-evaluator-first-location-score-KnownLocationRiskEvaluator` | `VERY_SMALL` | Score when the user has no known locations yet |
| `adaptive-evaluator-known-location-score-KnownLocationRiskEvaluator` | `NEGATIVE_LOW` | Score when city and country match a known location |
| `adaptive-evaluator-same-country-score-KnownLocationRiskEvaluator` | `VERY_SMALL` | Score when the country was seen before but the city is new |
| `adaptive-evaluator-new-country-score-KnownLocationRiskEvaluator` | `MEDIUM` | Score when the login country was never seen before |

Example for Browser:

| Realm attribute | Default | Effect |
|-----------------|---------|--------|
| `adaptive-evaluator-known-browser-score-BrowserRiskEvaluator` | `NEGATIVE_LOW` | Score when the user agent matches a default known browser |
| `adaptive-evaluator-unknown-browser-score-BrowserRiskEvaluator` | `MEDIUM` | Score when the user agent does not match a default known browser |

Example for Operating system:

| Realm attribute | Default | Effect |
|-----------------|---------|--------|
| `adaptive-evaluator-known-os-score-OperatingSystemRiskEvaluator` | `NEGATIVE_LOW` | Score for Linux, macOS, or recent Windows |
| `adaptive-evaluator-legacy-windows-score-OperatingSystemRiskEvaluator` | `MEDIUM` | Score for older Windows versions |
| `adaptive-evaluator-unknown-os-score-OperatingSystemRiskEvaluator` | `MEDIUM` | Score when the OS cannot be classified |

Example for Known IP address (login events):

| Realm attribute | Default | Effect |
|-----------------|---------|--------|
| `adaptive-evaluator-never-seen-ip-score-LoginEventIpAddressRiskEvaluator` | `HIGH` | Score when the current IP was never seen in login history |
| `adaptive-evaluator-frequent-ip-score-LoginEventIpAddressRiskEvaluator` | `NEGATIVE_LOW` | Score when the IP appears often in recent logins |
| `adaptive-evaluator-occasional-ip-score-LoginEventIpAddressRiskEvaluator` | `VERY_SMALL` | Score when the IP was seen before but not frequently |
| `adaptive-evaluator-min-login-events-LoginEventIpAddressRiskEvaluator` | `4` | Minimum login events before frequent/occasional classification (below: invalid) |
| `adaptive-evaluator-frequent-ip-threshold-divisor-LoginEventIpAddressRiskEvaluator` | `3` | IP is frequent when occurrences ≥ event count / divisor |

## Adding settings to an evaluator

1. **Factory** — declare fields in `getAdditionalAdminConfigProperties()` using `EvaluatorSettingProperties` (score, int, string) or `ProviderConfigurationBuilder` directly.
2. **Config class** — getters only: keys, defaults, and `EvaluatorSettingUtils` reads (`getScore`, `getInt`, `getPositiveInt`).
3. **Evaluator** — scoring logic uses the config getters instead of hardcoded constants.

The risk-based policies tab renders, persists, and hydrates these properties automatically. No extra UI wiring is required.

### Example: integer setting

```java
@Override
public List<ProviderConfigProperty> getAdditionalAdminConfigProperties() {
    return EvaluatorSettingProperties.of(
            EvaluatorSettingProperties.intProperty(
                    MyRiskEvaluator.class, "ttl-days",
                    "TTL (days)", "Help text.", 90, 0));
}
```

```java
int ttlDays = EvaluatorSettingUtils.getInt(realm, MyRiskEvaluator.class, "ttl-days", 90);
```

### Example: risk score setting

```java
EvaluatorSettingProperties.scoreProperty(
        MyRiskEvaluator.class, "known-score",
        "Known case score", "Risk score when the trust signal applies.",
        Risk.Score.NEGATIVE_LOW)
```

```java
Risk.Score score = EvaluatorSettingUtils.getScore(
        realm, MyRiskEvaluator.class, "known-score", Risk.Score.NEGATIVE_LOW);
```

## Legacy behavior

Realms without these attributes keep the built-in defaults. No migration is required when upgrading the extension.

Invalid or missing values fall back to defaults at runtime; authentication is not blocked by bad configuration. The admin tab validates risk score list fields when options match `EvaluatorSettingUtils.configurableScoreNames()`.

## Related work

When merging the IP whitelist extension, refactor `IpWhitelistEvaluatorConfig` to use `EvaluatorSettingUtils` instead of local score parsing.

See also [Realm setup](realm-setup.md).
