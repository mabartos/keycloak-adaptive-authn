# Evaluator settings

Risk evaluators can expose realm-level settings beyond **enabled** and **trust**. These are stored as realm attributes and edited in **Authentication → Risk-based policies** (requires `declarative-ui`).

## Naming convention

```
adaptive-evaluator-{settingKey}-{EvaluatorSimpleClassName}
```

Built with `RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey)`.

Example for Known location TTL:

| Realm attribute | Default | Effect |
|-----------------|---------|--------|
| `adaptive-evaluator-ttl-days-KnownLocationRiskEvaluator` | `90` | Days before a known location stops providing a trust signal |

## Adding settings to an evaluator

1. **Factory** — declare fields in `getAdditionalAdminConfigProperties()` using `EvaluatorSettingProperties` (score, int, string) or `ProviderConfigurationBuilder` directly.
2. **Config class** — read values at runtime with `EvaluatorSettingUtils` (`getScore`, `getInt`, `getPositiveInt`).
3. **Evaluator** — use the config class instead of hardcoded constants.

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
