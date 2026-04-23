![Keycloak](docs/img/adaptive-authentication-logo.png)

<img src="docs/img/adaptive-authn.gif" alt="Adaptive Authentication Demo" width="1050">

# Keycloak Adaptive Authentication

* Change **authentication requirements** in real-time based on wider context
* **Strengthen security** - Require **MORE** factors when user attempt is suspicious or accessing sensitive resources
* **Better User Experience** - Require **LESS** factors when risk of fraudulent user is low
* **Integration with remote services** - For more information about the user or helping evaluating data via remote
  services
* Gather **more information about user** in a secure way
* Uses **Risk-based** authentication
* Uses **AI services** for more complex risk evaluations

<img src="docs/img/github-risk-engine.png" alt="Risk Engine" width="1050"></img>

### Supported AI Engines

<table>
  <tr>
    <td align="center" width="200">
      <a href="https://chatgpt.com/">
        <img src="docs/img/chat-gpt-logo.png" width="80" alt="OpenAI ChatGPT logo"/>
      </a>
    </td>
    <td>
      <b><a href="https://chatgpt.com/">OpenAI ChatGPT</a></b><br/>
      Default model: <code>gpt-4o-mini</code><br/>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://www.anthropic.com/claude">
        <img src="docs/img/claude-logo.png" width="100" alt="Anthropic Claude logo"/>
      </a>
    </td>
    <td>
      <b><a href="https://www.anthropic.com/claude">Anthropic Claude</a></b><br/>
      Default model: <code>claude-haiku-4-5-20251001</code><br/>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://ai.google.dev/">
        <img src="docs/img/gemini-logo.png" width="80" alt="Google Gemini logo"/>
      </a>
    </td>
    <td>
      <b><a href="https://ai.google.dev/">Google Gemini</a></b> (free tier available)<br/>
      Default model: <code>gemini-2.5-flash-lite</code><br/>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://www.ibm.com/granite">
        <img src="docs/img/ibm-granite.png" width="90" alt="IBM Granite logo"/>
      </a>
    </td>
    <td>
      <b><a href="https://www.ibm.com/granite">IBM Granite</a></b> (deprecated)<br/>
      Default model: <code>granite-8b-code-instruct-128k</code><br/>
    </td>
  </tr>
</table>

Risk is evaluated **locally** as much as possible. AI engines are only used for complex evaluations that require deeper contextual analysis. Since AI services are **external dependencies**, all user data (IP addresses, device info) is **automatically anonymized** before being sent to prevent information leakage. See [Privacy and Anonymization Guide](docs/privacy-and-anonymization.md) for details.

For more information and setup, see the <a href="docs/ai-engine-integration.md">AI Integration Guide</a>.

## Supported Keycloak Versions

| Keycloak Version | Supported |
|------------------|-----------|
| 26.6.x           | Yes       |
| 26.5.x           | Yes       |
| main (nightly)   | Yes       |

## Getting Started

Build and try it out locally:

```shell
./mvnw -f core clean install -DskipTests -Pbuild-distribution
./mvnw exec:exec@start-server
```

The generated JAR (`core/target/keycloak-adaptive-authn-*.jar`) can be added to the [`/providers`](https://www.keycloak.org/server/configuration-provider#_installing_and_uninstalling_a_provider) folder of your Keycloak distribution. It is also available as a Maven package:

```xml
<dependency>
    <groupId>io.github.mabartos</groupId>
    <artifactId>keycloak-adaptive-authn</artifactId>
    <version>${version}</version>
    <type>jar</type>
</dependency>
```

For full build instructions, installation options, and configuration, see the [Start Guide](docs/start.md).

## Configuration

Configured in [`core/src/main/resources/application.properties`](core/src/main/resources/application.properties). Environment names match the `${VAR:default}` placeholders there.

| Environment variable | Application property | Default (when env unset) | Examples / accepted values | Purpose |
| --- | --- | --- | --- | --- |
| `KC_SPI_AI_ENGINE__PROVIDER` | `kc.spi-ai-engine--provider` | `granite` | `granite`, `gpt`, `claude`, `gemini` (SPI provider ids) | Default AI engine provider id (SPI). |
| `OPEN_AI_API_URL` | `ai.openai.api.url` | `https://api.openai.com/v1/chat/completions` | Any HTTPS URL to OpenAI-compatible chat completions endpoint | OpenAI Chat Completions endpoint URL. |
| `OPEN_AI_API_KEY` | `ai.openai.api.key` | *(empty)* | `sk-...` API secret | OpenAI API key. |
| `OPEN_AI_API_ORGANIZATION` | `ai.openai.api.organization` | *(empty)* | `org-...` if using org-scoped keys | Optional OpenAI organization id. |
| `OPEN_AI_API_PROJECT` | `ai.openai.api.project` | *(empty)* | `proj_...` if using project-scoped keys | Optional OpenAI project id. |
| `OPEN_AI_API_MODEL` | `ai.openai.api.model` | `gpt-4o-mini` | e.g. `gpt-4o-mini`, `gpt-4o` | OpenAI model name. |
| `GRANITE_API_URL` | `ai.granite.api.url` | *(empty)* | Base URL from your IBM Granite deployment | IBM Granite API URL. |
| `GRANITE_API_KEY` | `ai.granite.api.key` | *(empty)* | Service API key / token | IBM Granite API key. |
| `GRANITE_API_MODEL` | `ai.granite.api.model` | `granite-8b-code-instruct-128k` | Model id offered by your Granite endpoint | IBM Granite model name. |
| `CLAUDE_API_URL` | `ai.claude.api.url` | `https://api.anthropic.com/v1/messages` | Anthropic Messages API HTTPS URL | Anthropic Messages API URL. |
| `CLAUDE_API_KEY` | `ai.claude.api.key` | *(empty)* | `sk-ant-api03-...` | Anthropic API key. |
| `CLAUDE_API_MODEL` | `ai.claude.api.model` | `claude-haiku-4-5-20251001` | Any Claude model id supported by your key | Claude model name. |
| `CLAUDE_API_VERSION` | `ai.claude.api.version` | `2023-06-01` | Date string per Anthropic docs, e.g. `2023-06-01` | Anthropic API version header value. |
| `CLAUDE_API_ENABLE_CACHING` | `ai.claude.api.enable.caching` | `true` | `true` or `false` | Enable Anthropic prompt caching (`true` / `false`). |
| `GEMINI_API_URL` | `ai.gemini.api.url` | `https://generativelanguage.googleapis.com/v1beta` | Google Generative Language API base URL | Google Generative Language API base URL. |
| `GEMINI_API_KEY` | `ai.gemini.api.key` | *(empty)* | Google AI Studio / Cloud API key | Google Gemini API key. |
| `GEMINI_API_MODEL` | `ai.gemini.api.model` | `gemini-2.5-flash-lite` | e.g. `gemini-2.5-flash-lite`, `gemini-2.0-flash` | Gemini model name. |
| `AI_ANONYMIZE_ENABLED` | `ai.anonymize.enabled` | `true` | `true` or `false` | Anonymize user data before AI calls (`true` / `false`). |
| `RECAPTCHA_SITE_KEY` | `recaptcha.site.key` | *(empty)* | reCAPTCHA / Enterprise site key string | reCAPTCHA site key. |
| `RECAPTCHA_PROJECT_ID` | `recaptcha.project.id` | *(empty)* | GCP project id (reCAPTCHA Enterprise) | Google Cloud project id for reCAPTCHA Enterprise. |
| `RECAPTCHA_PROJECT_API_KEY` | `recaptcha.project.api.key` | *(empty)* | API key with reCAPTCHA Enterprise access | Google Cloud API key for reCAPTCHA Enterprise. |
| `KC_ADAPTIVE_TESTING_RANDOM_IP_ENABLED` | `ip.address.testing.random.enabled` | `false` | `true` or `false` | Controls randomness for the test IP provider (`TestIpAddressContext`). If `true` and `ip.address.testing.value` is **empty**, each lookup uses a **random** IP from the **built-in** list in code (5 fixed public IPs). If `true` and `ip.address.testing.value` has **two or more** comma-separated IPs, picks randomly among them. If `false` and `ip.address.testing.value` is set, always uses the **first** IP of that list. |
| `KC_ADAPTIVE_TESTING_IP_VALUE` | `ip.address.testing.value` | *(empty)* | One IP or comma-separated list, e.g. `203.0.113.1` or `1.1.1.1,8.8.8.8` | Optional override list (IPv4/IPv6). When non-empty, replaces the built-in pool for fixed or random selection (see `KC_ADAPTIVE_TESTING_RANDOM_IP_ENABLED`). When empty and random is `false`, legacy `ip.address.use.testing=true` can still force default `77.75.72.3`—see `TestIpAddressContext`. |
| `KC_ADAPTIVE_LOCATION_PROVIDERS` | `location.providers` | `ipapi-co-free` | Comma-separated ids (try order). **ipapi.co:** `ipapi-co-free` (no token), `ipapi-co-pro` (requires `KC_ADAPTIVE_IPAPI_TOKEN`). **ip-api.com:** `ip-api-com-free` (HTTP, no key), `ip-api-com-pro` (requires `KC_ADAPTIVE_IP_API_COM_API_KEY`). Example: `ipapi-co-free,ip-api-com-free`. | GeoIP resolver chain. If **every** resolver fails, location falls back to `UnknownLocationData` (all string attributes `Unknown`, lat/lon `null`, not cached, **ERROR** logged). See `GeoIpLocationContextFactory`. |
| `KC_ADAPTIVE_IPAPI_TOKEN` | `location.ipapi.token` | *(empty)* | ipapi.co paid-plan token (sent as `?token=`) | **Required** when `location.providers` includes `ipapi-co-pro`. Ignored for `ipapi-co-free`. Host is fixed to `https://ipapi.co`. |
| `KC_ADAPTIVE_IP_API_COM_API_KEY` | `location.ip-api-com.api-key` | *(empty)* | Pro `key` from [ip-api.com](https://ip-api.com) | **Required** when providers include `ip-api-com-pro` (HTTPS `https://pro.ip-api.com/json/{ip}?key=…`). Ignored for `ip-api-com-free`. |
| `KC_ADAPTIVE_LOCATION_GLOBAL_CACHE_TTL` | `location.global-cache.ttl` | `PT24H` | ISO-8601 duration, e.g. `PT1H`, `PT24H`, `P1D` | Global location cache entry TTL. |
| `KC_ADAPTIVE_LOCATION_GLOBAL_CACHE_MAXIMUM_SIZE` | `location.global-cache.maximum-size` | `10000` | Positive integer (max entries) | Global location cache maximum entries. |

## Resources with more info

1. **Unlocking Adaptive Authentication (most recent)**
   - [Keycloak DevDay](https://www.keycloak-day.dev/) @ Darmstadt, Germany 2025
   - [Slides](https://drive.google.com/file/d/12-vAuVmWqUb3581D8WqWq0uutLbH7tsn/view?usp=sharing)
   - [Talk](https://www.youtube.com/watch?v=TjanummQn7U)
2. **Adaptive Authentication**
    - [KeyConf24](https://keyconf.dev/) @ Vienna, Austria 2024
    - [Slides](https://drive.google.com/file/d/1PESlDBR8P9nQJyPz_H45R3ZS4LjtSV_W/view?usp=sharing)
    - [Talk](https://www.youtube.com/watch?v=0zWlc08CPuo)
    - [Demo](https://drive.google.com/file/d/1dv5zWM69-KZyT3OUjLe-3b1GcI8ErDJ2/view?usp=sharing)
3. **AI-powered Keycloak**
    - OpenShiftAI Roadshow @ Bratislava, Slovakia 2024
    - [Slides](https://drive.google.com/file/d/1WscEQlWpjYdrOwGDMj9IDV6bARY-4Utn/view?usp=sharing)
4. **Adaptive Authentication**
    - Master's thesis completed 2024
    - (Information might differ)
    - [Document](https://github.com/mabartos/adaptive-authn-docs/blob/main/Adaptive_Authentication_Final.pdf)

## Connected Authentication Policies

**NOTE**: Authentication policies that were part of this Adaptive authentication initiative were moved to
repository [mabartos/keycloak-authn-policies](https://github.com/mabartos/keycloak-authn-policies).
