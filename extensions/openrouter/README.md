# OpenRouter AI Engine Extension

AI engine provider for [Keycloak Adaptive Authentication](../../README.md) that routes risk evaluations through [OpenRouter](https://openrouter.ai/).

## What it does

OpenRouter exposes many models through a single OpenAI-compatible API. Operators can pick cost, latency, or capability without adding a new engine implementation per vendor.

The extension registers an `AiEngine` SPI provider with id `openrouter`, used by evaluators such as `AiAccountTakeoverEvaluator`.

## Installation

1. Build the project:

    ```shell
    mvn clean install -DskipTests
    ```

2. Copy the built JARs to your Keycloak [`providers/`](https://www.keycloak.org/server/configuration-provider#_installing_and_uninstalling_a_provider) directory:

    - `core/target/keycloak-adaptive-authn-*.jar`
    - `extensions/openrouter/target/keycloak-adaptive-ext-openrouter-*.jar`

3. Rebuild Keycloak to pick up the new providers:

    ```shell
    ${KEYCLOAK_HOME}/bin/kc.sh build
    ```

The extension is auto-discovered via Java's `ServiceLoader` mechanism (registered in `META-INF/services`).

## Configuration

### Provider selection

```bash
KC_SPI_AI_ENGINE__PROVIDER=openrouter
```

### Environment variables

Configuration is done through environment variables. SmallRye Config resolves them automatically; no `application.properties` entry is required in the extension JAR.

| Environment Variable | Description | Required | Default |
|---|---|---|---|
| `OPENROUTER_API_KEY` | API key from [openrouter.ai](https://openrouter.ai/) | Yes | _(none)_ |
| `OPENROUTER_API_URL` | Chat completions endpoint | No | `https://openrouter.ai/api/v1/chat/completions` |
| `OPENROUTER_API_MODEL` | Model slug from the [OpenRouter model list](https://openrouter.ai/models) | Yes | _(none)_ |

**Example (Docker):**

```shell
docker run ... \
  -e KC_SPI_AI_ENGINE__PROVIDER=openrouter \
  -e OPENROUTER_API_KEY=sk-or-... \
  -e OPENROUTER_API_MODEL=deepseek/deepseek-v4-flash \
  ...
```

### Getting an API key

1. Visit [openrouter.ai](https://openrouter.ai/)
2. Sign up or log in
3. Open **Keys** and create an API key

## Architecture

```
OpenRouterAiEngineFactory (SPI entry point)
  └── OpenRouterAiEngine
        ├── Reads URL, API key, and model from SmallRye Config
        ├── Calls OpenRouter chat completions API
        └── Maps JSON response to Risk via AiEngineUtils
```

## Community maintainer
- [Thomas DELORGE](https://github.com/thomasdelorge)