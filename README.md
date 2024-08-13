![Keycloak](docs/img/keycloak-adaptive-colored.png)

# Keycloak Adaptive Authentication Extension

## Getting started

### Container

You can use the container image by running:

    podman run -p 8080:8080 quay.io/mabartos/keycloak:adaptive start

This command starts Keycloak exposed on the local port 8080 (`localhost:8080`).
In order to see the functionality in action, navigate to `localhost:8080/realms/adaptive/account`.

NOTE: If you have installed Docker, use `docker` instead of `podman`.

### Building from Source

To build from source every module, refer to particular READMEs, or follow [building and working with the code base](docs/building-source.md) guide.