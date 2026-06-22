# AI Engine Integration

## Selecting the AI Engine

The AI engine provider is configured via the Keycloak SPI option:

```bash
KC_SPI_AI_ENGINE__PROVIDER=claude
```

Available core providers: `gpt`, `claude`, `gemini`, `granite` (deprecated).

Additional providers are available as extensions under [`extensions/`](../extensions/). See each extension README for installation.

## OpenAI ChatGPT

### Getting an API Key

1. Visit [platform.openai.com](https://platform.openai.com/)
2. Sign up or log in
3. Navigate to **API Keys** and create a new secret key (starts with `sk-`)

### Configuration

| Environment Variable | Required | Default |
|---|---|---|
| `OPEN_AI_API_KEY` | Yes | - |
| `OPEN_AI_API_URL` | No | `https://api.openai.com/v1/chat/completions` |
| `OPEN_AI_API_MODEL` | No | `gpt-4o-mini` |
| `OPEN_AI_API_ORGANIZATION` | No | - |
| `OPEN_AI_API_PROJECT` | No | - |

## Claude (Anthropic)

### Getting an API Key

1. Visit [console.anthropic.com](https://console.anthropic.com/)
2. Sign up or log in
3. Navigate to **API Keys** and generate a new key (starts with `sk-ant-`)

### Configuration

| Environment Variable | Required | Default |
|---|---|---|
| `CLAUDE_API_KEY` | Yes | - |
| `CLAUDE_API_URL` | No | `https://api.anthropic.com/v1/messages` |
| `CLAUDE_API_MODEL` | No | `claude-haiku-4-5-20251001` |
| `CLAUDE_API_VERSION` | No | `2023-06-01` |
| `CLAUDE_API_ENABLE_CACHING` | No | `true` |

## IBM Granite (Deprecated)

| Environment Variable | Required | Default |
|---|---|---|
| `GRANITE_API_KEY` | Yes | - |
| `GRANITE_API_URL` | Yes | - (must include `/chat/completions` suffix) |
| `GRANITE_API_MODEL` | No | `granite-8b-code-instruct-128k` |

> **Note:** IBM Granite may be slower than other providers, so the timeout for risk evaluations may need to be increased.

## Google Gemini

### Getting an API Key

1. Visit [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click **Get API Key** and generate a new key (starts with `AIzaSy...`)

A free tier is available (15 requests/minute for Flash).

### Configuration

| Environment Variable | Required | Default |
|---|---|---|
| `GEMINI_API_KEY` | Yes | - |
| `GEMINI_API_URL` | No | `https://generativelanguage.googleapis.com/v1beta` |
| `GEMINI_API_MODEL` | No | `gemini-2.5-flash-lite` |

## OpenRouter (extension)

[OpenRouter](https://openrouter.ai/) is provided as a separate extension module. Install the JAR from `extensions/openrouter/` alongside the core module, then set `KC_SPI_AI_ENGINE__PROVIDER=openrouter`.

See the [OpenRouter extension README](../extensions/openrouter/README.md) for installation, API key setup, and environment variables (`OPENROUTER_API_KEY`, `OPENROUTER_API_URL`, `OPENROUTER_API_MODEL`).
