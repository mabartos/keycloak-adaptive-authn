## Getting started

### Building from Source

To build it from source, execute this command:

```shell
./mvnw clean install -DskipTests
```

If you want to try it out, follow this:

1. Build it with profile `-Pbuild-distribution` as:

```shell
./mvnw -f core clean install -DskipTests -Pbuild-distribution
```
2. Prepare your `.env` file with necessary configuration (see `.env.example` for more info)

3. Start the server with deployed extension

```shell
./mvnw exec:exec@start-server
```

4. Then you can access [User account](`http://localhost:8080/realms/adaptive/account`) to see the functionality in action.

### Container

You can build your own containerized Keycloak installation with this extension as described in this
guide: [Add Keycloak Adaptive Authentication extension](https://github.com/mabartos/keycloak-quarkus-extensions/blob/main/examples/keycloak-adaptive-authn.md).

**NOTE**: This is an old release with the authentication policies that are not part of this repository anymore.
Recommended way is to build it from source for now or follow steps on the guide mentioned above.

You can use the container image by running:

    podman run -p 8080:8080 quay.io/mabartos/keycloak-adaptive-all start

This command starts Keycloak exposed on the local port 8080 (`localhost:8080`).

In order to see the functionality in action, navigate to `localhost:8080/realms/authn-policy-adaptive/account`.

ℹ️ **INFO:** If you want to use the OpenAI capabilities, set the environment variables (by setting `-e OPEN_AI_API_*`)
for the image described in the [README](adaptive/README.md#integration-with-openai) of the `adaptive` module..

ℹ️ **INFO:** If you have installed Docker, use `docker` instead of `podman`.

## Show example flow

In order to see the execution of the authentication flow from the example realm `adaptive`, just access the url
`http://localhost:8080/admin/adaptive/console/`.

## Integration with OpenAI

In order to use the default OpenAI engine for risk scoring, create `.env` file in the working directory, or set
following environment variables:

- `OPEN_AI_API_KEY` - OpenAI API key
- `OPEN_AI_API_ORGANIZATION` - OpenAI organization ID
- `OPEN_AI_API_PROJECT` - OpenAI project ID
- `OPEN_AI_API_URL`(optional) - OpenAI URL (default 'https://api.openai.com/v1/chat/completions') (with the suffix
  `/chat/completions`)
- `OPEN_AI_API_MODEL`(optional) - OpenAI Model type (default `gpt-3.5-turbo`)

## Integration with IBM Granite

In order to use the IBM Granite NLP engine for risk scoring, create `.env` file in the working directory, or set
following environment variables:

- `GRANITE_API_KEY` - Granite API key
- `GRANITE_API_URL` - Granite API URL (with the suffix `/chat/completions`)
- `GRANITE_API_MODEL`(optional) - Granite API Model (default `granite-8b-code-instruct-128k`)

As the IBM Granite is not the default AI NLP engine used in this extension, the default provider needs to be set (and
the `build` command executed):

- `KC_SPI_AI_ENGINE_PROVIDER=granite`

**WARNING**: It seems the IMB Granite is slower than OpenAI ChatGPT, so the timeout for risk evaluations needs to be
increased for now.