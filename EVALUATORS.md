# Risk Evaluators

> Auto-generated — do not edit manually.

## Before Authentication (`BEFORE_AUTHN`)

Executed before the user is known. Useful for evaluating risk from browser, IP address, device, etc.

| Evaluator | Source | Description |
|-----------|--------|-------------|
| Browser | Core | Scores the browser from the login request user agent. Chrome, Firefox, and Safari reduce risk, other browsers score moderate risk. |
| Client sensitivity | Core | Scores risk from the requesting OAuth client's sensitivity. Configure per client under Client → Risk-based settings. |
| Init location | Core | Prepares GeoIP/location context for later evaluators. |
| Operating system | Core | Scores the operating system from the login request user agent. Linux, macOS, and Windows 10/11 reduce risk, older Windows versions score moderate risk. |
| reCAPTCHA | Core | Uses Google reCAPTCHA Enterprise risk scores for the login attempt. |

## User Known (`USER_KNOWN`)

Executed after identifying the user during authentication (e.g. after username + password). Useful for evaluating risk from user roles, login failures, login events, etc.

| Evaluator | Source | Description |
|-----------|--------|-------------|
| AI account takeover | Core | LLM behavioral analysis for account takeover (anonymized). |
| Client role | Core | Scores risk from the user's roles on the requesting OAuth client using built-in prefix heuristics (manage-*, create-*, view-*, query-*, and selected admin roles). |
| Failed login pattern | Core | Detects distributed attack patterns and bot-like timing in login failure events. |
| Known IP address | Core | Scores whether the current IP was seen in the user's successful login history. New or rare IPs increase risk, familiar IPs can reduce it. |
| Known location | Core | Compares the current login location (GeoIP) to the user's known locations after identification. Requires location context, enable Init location if this evaluator is active. |
| Login failures | Core | Increases risk from recent LOGIN_ERROR events for the user (failure count, recency, and IP mismatch). Uses the Keycloak event store, not the brute-force counter. |
| Realm role | Core | Scores realm roles assigned to the user using built-in prefix heuristics (manage-*, create-*, view-*, query-*, and selected admin roles). |
| Unusual login time | Core | Scores login attempts outside the user's typical time-of-day/weekday pattern (learned from history). |
| SSF signal | Extension | Scores risk from Shared Signals Framework (SSF) events when the SSF extension is available. |

## Continuous (`CONTINUOUS`)

Re-evaluated at runtime when events occur and the risk score for the authenticated user should be recalculated. Should be used in conjunction with an event listener.

| Evaluator | Source | Description |
|-----------|--------|-------------|
| Concurrent sessions | Core | Detects many concurrent sessions or spread across IPs for the same user during the session. Useful for session hijacking or shared-credential abuse after login. |
| User actions | Core | Scores bursts of sensitive account events (email change, password reset, credential changes, etc) in the continuous evaluation phase. |

---

Rows marked **Extension** come from optional modules under [extensions](extensions/) 
when present on the generator classpath (currently SSF). 
Backend-only modules such as `ip-api` (GeoIP) and `openrouter` (AI engine) do not register evaluators.

---

**Note:** This file is auto-generated. To regenerate it, run:

```bash
mvn -pl utils -am install -Dmaven.test.skip=true && mvn -pl utils exec:java@generate-evaluators-doc
```

