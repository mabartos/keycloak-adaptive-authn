![Keycloak](docs/img/keycloak-adaptive-colored.png)

# Keycloak Adaptive Authentication Extension

### Supported AI NLP Engines:

- **OpenAI ChatGPT** - (preview)
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

## Resources with more info

1. **Adaptive Authentication**
    - [KeyConf24](https://keyconf.dev/) @ Vienna, Austria
    - [Slides](https://drive.google.com/file/d/1PESlDBR8P9nQJyPz_H45R3ZS4LjtSV_W/view?usp=sharing)
    - [Talk](https://www.youtube.com/watch?v=0zWlc08CPuo)
    - [Demo](https://drive.google.com/file/d/1dv5zWM69-KZyT3OUjLe-3b1GcI8ErDJ2/view?usp=sharing)
2. **AI-powered Keycloak**
    - OpenShiftAI Roadshow @ Bratislava, Slovakia
    - [Slides](https://drive.google.com/file/d/1WscEQlWpjYdrOwGDMj9IDV6bARY-4Utn/view?usp=sharing)
3. **Adaptive Authentication**
    - Master's thesis completed 2024
    - (Information might differ)
    - [Document](https://github.com/mabartos/adaptive-authn-docs/blob/main/Adaptive_Authentication_Final.pdf)
