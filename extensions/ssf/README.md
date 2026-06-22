# SSF Signal Extension (Proof of Concept)

> **This extension is a Proof of Concept (PoC).**
> It demonstrates how [Shared Signals Framework (SSF)](https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html) events could be incorporated into adaptive authentication risk scoring.
> The signal types, risk scoring model, and storage mechanism are subject to change as the SSF ecosystem in Keycloak matures.

Risk evaluator and context for [Keycloak Adaptive Authentication](../../README.md).

> **Note:** This extension requires the SSF experimental feature in Keycloak, which is only available in upstream (unreleased) builds.
> It is **not available in Keycloak 26.x releases**.
> The extension auto-disables itself on Keycloak versions that lack SSF support.

## Status

**PoC — waiting for SSF Receiver support in Keycloak.**

Keycloak currently only acts as an SSF **transmitter** — it sends security events to external receivers but cannot yet receive incoming SSF events from external identity providers. Without a receiver, this extension cannot be wired end-to-end; it provides the risk-scoring side and a `recordSignal()` integration point, ready to be connected once the receiver lands.

The SSF Receiver feature is tracked in: [keycloak/keycloak#43614 — Add Support for SSF Receivers with Push Delivery](https://github.com/keycloak/keycloak/issues/43614)

Once the receiver lands, it will provide an `SsfEventListener` SPI that this extension can hook into to record incoming signals via `SsfSignalContext.recordSignal()` and feed them into risk scoring at authentication time.

## What it does

The extension evaluates risk based on SSF security signals stored for a user. Each signal carries a type, timestamp, source, and initiating entity. Risk is scored by combining signal severity with recency:

| Signal Type | Description | Within 1h | 1-24h | 1-7d | Older |
|---|---|---|---|---|---|
| `ACCOUNT_RECOVERY_ACTIVATED` | Recovery flow triggered (possible takeover) | EXTREME | VERY_HIGH | HIGH | MEDIUM |
| `CREDENTIAL_REVOKED` | Credential explicitly revoked | VERY_HIGH | HIGH | MEDIUM | SMALL |
| `ACCOUNT_DISABLED` | Account disabled externally | VERY_HIGH | HIGH | MEDIUM | SMALL |
| `SESSION_REVOKED` | Session terminated by external system | HIGH | MEDIUM | SMALL | VERY_SMALL |
| `CREDENTIAL_CHANGE` | Credential updated (password reset, rotation) | MEDIUM | SMALL | VERY_SMALL | NONE |
| `ASSURANCE_LEVEL_DECREASED` | MFA downgraded to single-factor | MEDIUM | SMALL | VERY_SMALL | NONE |
| `DEVICE_COMPLIANCE_CHANGED` | Device no longer compliant | MEDIUM | SMALL | VERY_SMALL | NONE |

Multiple signals (3+ within 24h) bump the final risk score by one level (capped at EXTREME).
A clean history (no signals) produces a `NEGATIVE_LOW` trust signal.

## Architecture

```
SsfSignalRiskEvaluatorFactory (SPI entry point, @EvaluationPhase(USER_KNOWN))
  └── SsfSignalRiskEvaluator
        └── SsfSignalContext (reads signals from user attributes)
              └── SsfSignalData (type, timestamp, source, initiating entity)

SsfSignalContext.recordSignal(user, signal)  ← future integration point for SSF Receiver
```

Signal types reference CAEP event type URIs from `keycloak-ssf-core` (`CaepCredentialChange.TYPE`, `CaepSessionRevoked.TYPE`), and the `InitiatingEntity` enum is reused from Keycloak's SSF module.

Signals are stored as user attributes (`adaptive-ssf-signals`) with FIFO eviction at 20 entries.

## Build

This extension depends on `keycloak-ssf-core`, which is only available in upstream Keycloak (`999.0.0-SNAPSHOT`), not in released versions like 26.x.

The `ssf` Maven profile is **active by default**. Pass `-Dssf.skip` to exclude it when building against a released Keycloak version:

```shell
# Default — builds the SSF extension (upstream Keycloak)
mvn clean install -DskipTests

# Skip the SSF extension (required for released Keycloak, e.g. 26.6.3)
mvn clean install -DskipTests -Dkeycloak.version=26.6.3 -Dssf.skip
```

## Installation

1. Build the project with upstream Keycloak:

    ```shell
    mvn clean install -DskipTests
    ```

2. Copy the built JAR (`extensions/ssf/target/keycloak-adaptive-ext-ssf-*.jar`) along with the core module JAR to your Keycloak's [`providers/`](https://www.keycloak.org/server/configuration-provider#_installing_and_uninstalling_a_provider) directory.

3. Enable the SSF experimental feature and rebuild Keycloak:

    ```shell
    ${KEYCLOAK_HOME}/bin/kc.sh build --features=ssf
    ```

The extension is auto-discovered via Java's `ServiceLoader` mechanism (registered in `META-INF/services`).
