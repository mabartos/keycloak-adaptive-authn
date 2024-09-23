![Keycloak](docs/img/keycloak-adaptive-colored.png)

# Keycloak Adaptive Authentication Extension

### Supported AI NLP Engines:

- **OpenAI ChatGTP** - (preview)
- **IBM Granite** - (experimental)

For more information, refer to the [README](adaptive/README.md) in `adaptive` module.

## Getting started

### Container

You can use the container image by running:

    podman run -p 8080:8080 quay.io/mabartos/keycloak-adaptive-all start

This command starts Keycloak exposed on the local port 8080 (`localhost:8080`).

In order to see the functionality in action, navigate to `localhost:8080/realms/authn-policy-adaptive/account`.

ℹ️ **INFO:** If you want to use the OpenAI capabilities, set the environment variables (by setting `-e OPEN_AI_API_*`) for the image described in the [README](adaptive/README.md#integration-with-openai) of the `adaptive` module..

ℹ️ **INFO:** If you have installed Docker, use `docker` instead of `podman`.

### Building from Source

To build from source every module, refer to particular READMEs, or follow [building and working with the code base](docs/building-source.md) guide.