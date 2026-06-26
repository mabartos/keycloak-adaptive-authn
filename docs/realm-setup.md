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

To persist risk evaluation summaries (login and continuous remediation) in the user event store:

1. In **Authentication → Risk-based policies**, enable **Risk evaluation audit events**.
2. In **Realm settings → Events → User events settings**:
   - **Save user events** — `ON`
   - **Saved event types** — add **`Custom required action`** (Keycloak has no extension-specific event type yet, see [Custom EventType #15288](https://github.com/keycloak/keycloak/issues/15288)).

After a login, one event is stored with detail:

- `custom_required_action=adaptive-risk-evaluation`
- Numeric scores and simple levels (`adaptive_*_level`: `LOW` / `MEDIUM` / `HIGH`) for `BEFORE_AUTHN` (if run), `USER_KNOWN`, and overall risk
- Per-evaluator decision (one `Evaluator=SCORE` or `Evaluator=INVALID:reason` per line, sorted by severity).

When continuous evaluation revokes sessions (score ≥ threshold), one event is stored with `custom_required_action=adaptive-risk-remediation`, `adaptive_phase=CONTINUOUS`, `adaptive_remediation=sessions_revoked`, continuous score, `adaptive_continuous_level`, and evaluators.

Filter in **Events → User events** by those detail values (`adaptive-risk-evaluation` vs `adaptive-risk-remediation`) to distinguish login audit, remediation, and real required actions.

### 3. Client role scores (`ClientRoleRiskEvaluator`, optional)

Per client role, set attribute `adaptive-client-role-riskScore` (Admin Console: **Clients → {client} → Roles → {role} → Attributes**, or realm import / REST).

| Attribute | Example |
|-----------|---------|
| `adaptive-client-role-riskScore` | `HIGH` |

Allowed values match `Risk.Score` (`VERY_SMALL`, `SMALL`, `NONE`, `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`, `NEGATIVE_LOW`, etc.). `INVALID` is not allowed.

- **Missing attribute** — role ignored at login (WARN if assigned).
- **Explicit `NONE`** — intentional neutral; stored on the role as proof of configuration, ignored for scoring.
- **No scorable assigned role** — `ClientRoleRiskEvaluator` returns `invalid` for that dimension.

Role scoring is evaluated at login only (`ClientRoleRiskEvaluator`, phase `USER_KNOWN`).
