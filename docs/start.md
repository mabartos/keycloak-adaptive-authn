## Getting started

Let's try the Adaptive Authentication! 🚀

<img src="img/adaptive-authn-login-screen.png" alt="Adaptive Authentication Login Screen" width="500">

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
2. Build extensions and copy them to the distribution:

```shell
./mvnw -f extensions clean install -DskipTests
./mvnw exec:exec@copy-extensions
```

3. Prepare your `.env` file with necessary configuration (see `.env.example` for more info)

4. Start the server with deployed extension

```shell
./mvnw exec:exec@start-server
```

4. Then you can access [User account](http://localhost:8080/realms/adaptive/account) to see the functionality in action.

### Building with Different Keycloak Versions

The project uses **Keycloak nightly builds (999.0.0-SNAPSHOT) by default**. All dependencies and the distribution are fetched from GitHub releases. You can also build with any specific released version.

#### Use Nightly Build (Default)
```shell
./mvnw -f core clean install -DskipTests -Pbuild-distribution
```
- Uses Keycloak version: `999.0.0-SNAPSHOT` for all dependencies
- Downloads distribution from: `https://github.com/keycloak/keycloak/releases/download/nightly/keycloak-999.0.0-SNAPSHOT.zip`

#### Use Specific Release

To build with a specific Keycloak release, set both the version and the GitHub tag:

Example for Keycloak 26.5.2:
```shell
./mvnw -f core clean install -DskipTests -Pbuild-distribution \
  -Dkeycloak.version=26.5.2 \
  -Dkeycloak.github.tag=26.5.2
```
- Uses Keycloak version: `26.5.2` for all dependencies
- Downloads distribution from: `https://github.com/keycloak/keycloak/releases/download/26.5.2/keycloak-26.5.2.zip`

Example for Keycloak 26.5.4:
```shell
./mvnw -f core clean install -DskipTests -Pbuild-distribution \
  -Dkeycloak.version=26.5.4 \
  -Dkeycloak.github.tag=26.5.4
```

#### Running Tests with a Specific Release

When testing against a released version, pass `-Dkeycloak.test.realm.source=released` to switch to the released realm config:

```shell
# Build (skip test execution)
./mvnw clean install -Dmaven.test.skip=true -Pbuild-distribution,dist-maven \
  -Dkeycloak.version=26.6.2 -Dkeycloak.test.realm.source=released

# Run integration tests
./mvnw -f tests test \
  -Dkeycloak.version=26.6.2 -Dkeycloak.test.realm.source=released
```

Without `-Dkeycloak.test.realm.source=released`, the build defaults to `nightly` and tries to resolve dependencies only available in SNAPSHOT.

**Key Points:**
- **Nightly is default** - Both Maven dependencies and the distribution use `999.0.0-SNAPSHOT` from the `nightly` tag
- **For specific versions** - Set both `-Dkeycloak.version` and `-Dkeycloak.github.tag` to the same version
- **For tests with specific versions** - Add `-Dkeycloak.test.realm.source=released`
- **Downloads are cached** in `~/.m2/repository/.cache/keycloak/{tag}/` for faster rebuilds
- **All distributions come from GitHub** - no Maven Central dependency

**Clear Download Cache:**

If you need to re-download a fresh copy:
```shell
rm -rf ~/.m2/repository/.cache/keycloak/
```

### Container

You can build a custom Keycloak container image with the extension by adding the generated JAR to the `/opt/keycloak/providers` directory:

```dockerfile
FROM quay.io/keycloak/keycloak:latest AS builder

COPY core/target/keycloak-adaptive-authn-*.jar /opt/keycloak/providers/
COPY extensions/*/target/*.jar /opt/keycloak/providers/

ENV KC_HEALTH_ENABLED=true
ENV KC_TRACING_ENABLED=true
ENV KC_FEATURES=declarative-ui

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
```

For more details on optimized images, see the [Keycloak Container Guide](https://www.keycloak.org/server/containers#_writing_your_optimized_keycloak_containerfile).

Build and run:

```shell
podman build -t keycloak-adaptive .
podman run -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  keycloak-adaptive start-dev
```

**INFO:** If you have installed Docker, use `docker` instead of `podman`.

## Realm Setup

If you want to use the extension on your **own existing realm** instead of the provided example realm, see the [Realm Setup Guide](realm-setup.md) for the required Events configuration.

## Show example flow

In order to see the execution of the authentication flow from the example realm `adaptive`, just access the url
`http://localhost:8080/admin/adaptive/console/`.

## AI Engine Integration

For configuring AI engines (OpenAI, Claude, Gemini, IBM Granite), see the [AI Engine Integration Guide](ai-engine-integration.md).