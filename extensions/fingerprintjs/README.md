# FingerprintJS Known Device Extension

Browser device fingerprinting : collects a FingerprintJS visitor identifier during login, evaluates it against per-user known devices, and persists trusted devices on the user profile.

## What it does

During authentication, the extension:

1. Runs the Device fingerprint collector execution in the browser flow (before risk evaluation)
2. Stores the visitor ID in the authentication session
3. Evaluates the fingerprint in the `USER_KNOWN` phase via `KnownDeviceRiskEvaluator`
4. Registers the device on successful login (`adaptive-device-known` user attribute)

Risk scoring (aligned with `KnownLocationRiskEvaluator`):

| Situation | Score |
|---|---|
| Known active device | `NEGATIVE_LOW` (trust) |
| First tracked device (no active devices in profile) | `VERY_SMALL` |
| Unknown device (other active devices exist) | `MEDIUM` |
| Fingerprint missing, no device history | `VERY_SMALL` (fail-open) |
| Fingerprint missing, has device history | `MEDIUM` |

## Installation

1. Build the project:

    ```shell
    mvn clean install -DskipTests
    ```

2. Copy the built JAR (`extensions/fingerprintjs/target/keycloak-adaptive-ext-fingerprintjs-*.jar`) along with the core module JAR to your Keycloak [`providers/`](https://www.keycloak.org/server/configuration-provider#_installing_and_uninstalling_a_provider) directory.

3. Rebuild Keycloak:

    ```shell
    ${KEYCLOAK_HOME}/bin/kc.sh build
    ```

Providers are auto-discovered via Java `ServiceLoader` (`META-INF/services`).

## Browser flow setup

Add the Device fingerprint collector execution to your browser authentication flow, before the `default-risk-evaluator` step (same position as reCAPTCHA). The collector skips itself when `KnownDeviceRiskEvaluator` is disabled for the realm.

Enable the evaluator in Realm settings → Risk-based policies (phase: After user identification).

## Configuration

Configuration uses realm attributes set from the admin UI (Risk-based policies tab, Known Device section).

| Realm attribute | Description | Default |
|---|---|---|
| `adaptive-evaluator-ttl-days-KnownDeviceRiskEvaluator` | Days before a known device stops providing a trust signal. `0` disables expiration. | `90` |
| `adaptive-evaluator-max-stored-devices-KnownDeviceRiskEvaluator` | Maximum number of known devices kept per user. Oldest entries are dropped first. | `10` |

Expired entries are excluded from the active set during evaluation (`KnownDeviceContext.initData`). If no active devices remain (for example after TTL expiry), the next login with a valid fingerprint is scored as a first tracked device (`VERY_SMALL`), not as an unknown device. On successful login, expired entries are pruned and the current device is registered again. If other active devices still exist, a login from an expired visitor ID is scored as an unknown device (`MEDIUM`).

## Privacy

- FingerprintJS v5.2.0 (MIT) is bundled in the extension theme and loaded from the Keycloak server.
- `monitoring: false` when initializing FingerprintJS (no telemetry to FingerprintJS).
- Only a hashed visitor identifier is sent to the server.

See [privacy-and-anonymization.md](../../docs/privacy-and-anonymization.md#device-fingerprinting) for details.

## Architecture

```
DeviceFingerprintCollectorFactory (Authenticator SPI)
  └── DeviceFingerprintCollector
        └── Stores visitor ID in auth session auth note

KnownDeviceRiskEvaluatorFactory (RiskEvaluator SPI)
  └── KnownDeviceRiskEvaluator (@EvaluationPhase USER_KNOWN)
        ├── Reads auth note visitor ID
        └── Compares to KnownDeviceContext

KnownDeviceContextFactory (UserContext SPI)
  └── KnownDeviceContext
        ├── initData: active (non-expired) devices from user attribute
        └── onSuccessfulLogin: register current device
```

## Components

| Provider ID | Type |
|---|---|
| `device-fingerprint-collector` | Authenticator |
| `known-device-risk-evaluator` | Risk evaluator |
| `known-device-context` | User context |

User attribute: `adaptive-device-known` (format: `visitorId` or `visitorId:epochSeconds`, via `Time.currentTime()`).

Realm attribute keys: `adaptive-evaluator-ttl-days-KnownDeviceRiskEvaluator`,
`adaptive-evaluator-max-stored-devices-KnownDeviceRiskEvaluator`
(same pattern as known locations, see `RiskEvaluatorFactory.getAdditionalSettingConfig`).

## Community maintainer
- [Thomas DELORGE](https://github.com/thomasdelorge)
