# Risk Evaluators

> Auto-generated — do not edit manually.
> Regenerate: `mvn -pl core compile exec:java@generate-evaluators-doc`

## Before Authentication (`BEFORE_AUTHN`)

Executed before the user is known. Useful for evaluating risk from browser, IP address, device, etc.

| Evaluator | Description |
|-----------|-------------|
| Browser | Scores the browser from the login request user agent. Chrome, Firefox, and Safari reduce risk, other browsers score moderate risk. |
| Client sensitivity | Scores risk from the requesting OAuth client's sensitivity. Configure per client under Client → Risk-based settings. |
| Init location | Prepares GeoIP/location context for later evaluators. |
| Operating system | Scores the operating system from the login request user agent. |
| reCAPTCHA | Uses Google reCAPTCHA Enterprise risk scores for the login attempt. |

## User Known (`USER_KNOWN`)

Executed after identifying the user during authentication (e.g. after username + password). Useful for evaluating risk from user roles, login failures, login events, etc.

| Evaluator | Description |
|-----------|-------------|
| AI account takeover | LLM behavioral analysis for account takeover (anonymized). |
| Client role | Scores risk from the user's assigned roles on the requesting OAuth client using the adaptive-client-role-riskScore attribute on each client role. Configure per role under Clients → Roles → Attributes. |
| Failed login pattern | Detects distributed attack patterns and bot-like timing in login failure events. |
| Known IP address | Scores whether the current IP was seen in the user's successful login history. New or rare IPs increase risk, familiar IPs can reduce it. |
| Known location | Compares the current login location (GeoIP) to the user's known locations after identification. Requires location context, enable Init location if this evaluator is active. |
| Login failures | Increases risk from recent LOGIN_ERROR events for the user (failure count, recency, and IP mismatch). Uses the Keycloak event store, not the brute-force counter. |
| Realm role | Scores realm roles assigned to the user using built-in prefix heuristics (manage-*, create-*, view-*, query-*, and selected admin roles). |
| Unusual login time | Scores login attempts outside the user's typical time-of-day/weekday pattern (learned from history). |

## Continuous (`CONTINUOUS`)

Re-evaluated at runtime when events occur and the risk score for the authenticated user should be recalculated. Should be used in conjunction with an event listener.

| Evaluator | Description |
|-----------|-------------|
| Concurrent sessions | Detects many concurrent sessions or spread across IPs for the same user during the session. Useful for session hijacking or shared-credential abuse after login. |
| User actions | Scores bursts of sensitive account events (email change, password reset, credential changes, etc) in the continuous evaluation phase. |

---

Additional evaluators are available in the [extensions](extensions/) directory.

---

**Note:** This file is auto-generated. To regenerate it, run:

```bash
mvn -pl core compile exec:java@generate-evaluators-doc
```

