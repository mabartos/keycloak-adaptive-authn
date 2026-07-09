# IP Whitelist Extension

Optional Keycloak Adaptive Authentication extension that adds a **BEFORE_AUTHN** risk evaluator for trusted IPv4 addresses.

## What it does

When the client IP matches a configured whitelist (single address, hyphen range, or CIDR), the evaluator emits a trust signal (default `NEGATIVE_LOW`). Non-matching IPs return `NONE` by default.

## Configuration (Admin Console)

All realm settings for this evaluator are edited in **Authentication → Risk-based policies**, section **Before authentication** (`BEFORE_AUTHN`).

Requires the Keycloak `declarative-ui` feature (see [Deploy](#deploy)).

| Tab field | Purpose | Default |
|-----------|---------|---------|
| **IP whitelist** (enable) | Turn the evaluator on or off | enabled |
| **IP whitelist trust** | Weight in the combined risk score | `1.0` |
| **IPv4 whitelist** | Trusted addresses (comma-separated) | (empty) |
| **Whitelisted IP score** | Score when the client IP matches | `NEGATIVE_LOW` |
| **Non-whitelisted IP score** | Score when the client IP does not match | `NONE` |

On save, values are stored as **realm attributes** (same keys as in the table below). The tab is the supported way to configure the extension; realm attributes are the runtime source of truth.

Invalid score values fall back to defaults at runtime. An empty or fully unparseable whitelist makes the evaluator return `INVALID` (excluded from score aggregation).

### Whitelist formats (IPv4 only)

Enter comma-separated entries in **IPv4 whitelist**:

| Format | Example |
|--------|---------|
| Single address | `203.0.113.42` |
| Hyphen range | `10.0.0.1-10.0.0.255` |
| CIDR | `10.0.0.0/8` |

Invalid entries are ignored at runtime (WARN log). IPv6 is not supported; IPv4-mapped IPv6 clients (`::ffff:…`) are normalized when possible.

## Build

```bash
./mvnw package -pl extensions/ip-whitelist -am
```

## Deploy

1. Copy `extensions/ip-whitelist/target/keycloak-adaptive-ext-ip-whitelist-*.jar` to Keycloak `providers/` (with the core adaptive authn JAR).
2. Start Keycloak with `KC_FEATURES=declarative-ui`.
3. Open **Authentication → Risk-based policies** → **Before authentication**.
4. Enable **IP whitelist**, set **IPv4 whitelist** and scores, then save.

See also [Realm setup](../../docs/realm-setup.md) for server prerequisites.

## Realm attributes (reference)

These keys are written by the risk-based policies tab. Use the Admin REST API only for automation or bulk import.

| Attribute | Default | Tab field |
|-----------|---------|-----------|
| `adaptive-evaluator-enabled-IpWhitelistRiskEvaluator` | `true` | IP whitelist (enable) |
| `adaptive-evaluator-trust-IpWhitelistRiskEvaluator` | (empty → full trust) | IP whitelist trust |
| `adaptive-evaluator-ipv4-whitelist-IpWhitelistRiskEvaluator` | (empty) | IPv4 whitelist |
| `adaptive-evaluator-whitelisted-score-IpWhitelistRiskEvaluator` | `NEGATIVE_LOW` | Whitelisted IP score |
| `adaptive-evaluator-not-whitelisted-score-IpWhitelistRiskEvaluator` | `NONE` | Non-whitelisted IP score |

Score values use `Risk.Score` enum names. `INVALID` is not accepted.

### Example: Admin REST API (optional)

```bash
curl -X PUT "https://keycloak.example/admin/realms/myrealm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "adaptive-evaluator-ipv4-whitelist-IpWhitelistRiskEvaluator": "10.0.0.0/8,203.0.113.42",
      "adaptive-evaluator-whitelisted-score-IpWhitelistRiskEvaluator": "NEGATIVE_LOW",
      "adaptive-evaluator-not-whitelisted-score-IpWhitelistRiskEvaluator": "NONE"
    }
  }'
```

## Cache

Parsed whitelists are cached in-memory (Caffeine) keyed by realm id and the raw whitelist attribute value. Tab or API updates take effect on the next evaluation without waiting for TTL expiry.

| Property | Default | Description |
|----------|---------|-------------|
| `ip-whitelist.cache.ttl` | `PT1H` | Cache entry TTL (ISO-8601 duration) |
| `ip-whitelist.cache.maximum-size` | `1000` | Max cached whitelist configs |

Set via `application.properties` / Quarkus config, not the risk-based policies tab.

## Community maintainer
- [Thomas DELORGE](https://github.com/thomasdelorge)
