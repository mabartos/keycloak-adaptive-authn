## Realm Setup for Adaptive Authentication

If you are adding the extension to an **existing realm** (rather than importing the provided `adaptive-realm.json`), you need to configure the following settings.

### 1. Server Configuration

The `declarative-ui` feature must be enabled on the Keycloak server. It is required for the **Risk-based policies** configuration tab in the Admin Console.

```
kc.sh start --features=declarative-ui
```

Or via environment variable:

```
KC_FEATURES=declarative-ui
```

### 2. Events Configuration

The extension relies on login events to track authentication attempts and compute risk scores.

In the Admin Console, go to **Realm Settings > Events > User events settings**:

- **Save user events** - `ON`
- **Expiration** - set to a reasonable retention period (e.g. `730` days / 2 years)
- **Saved event types** - add at least: `LOGIN`, `LOGIN_ERROR`

Under **Event listeners**, add:

- `login-events-adaptive-authn`

This listener captures login events used by the risk engine for evaluating user behavior.

#### Risk evaluation audit (optional)

To persist one risk summary per login in the user event store :

1. In **Saved event types**, add **`Custom required action`** (Keycloak has no extension-specific event type yet, see [Custom EventType #15288](https://github.com/keycloak/keycloak/issues/15288)).
2. Complete a login: after the `USER_KNOWN` step, one event is stored with detail :
    - `custom_required_action=adaptive-risk-evaluation`
    - Numeric scores and simple levels (`adaptive_*_level`: `LOW` / `MEDIUM` / `HIGH`) for `BEFORE_AUTHN` (if run, `USER_KNOWN`, and overall risk
    - Per-evaluator decision (one `Evaluator=SCORE` or `Evaluator=INVALID:reason` per line, sorted by severity).
3. When continuous evaluation revokes sessions (score ≥ threshold), one event is stored with `custom_required_action=adaptive-risk-remediation`, `adaptive_phase=CONTINUOUS`, `adaptive_remediation=sessions_revoked`, continuous score, `adaptive_continuous_level`, and evaluators.

Filter in **Events** > **User events** by those detail values (`adaptive-risk-evaluation` vs `adaptive-risk-remediation`) to distinguish login audit, remediation, and real required actions.
