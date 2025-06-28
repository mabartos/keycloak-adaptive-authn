![Keycloak](docs/img/keycloak-adaptive-colored.png)

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
    <th align="center">
      <img width="441" height="1">
      <a href="https://chatgpt.com/">OpenAI ChatGPT</a><p></p>
    </th>
    <th align="center">
      <img width="441" height="1">
      <a href="https://www.ibm.com/granite">IBM Granite</a> (experimental)<p></p>
    </th>
  </tr>
  <tr>
    <td align="center">
      <a href="https://chatgpt.com/">
        <img src="docs/img/chat-gpt-logo.png" width="250" alt="OpenAI ChatGPT logo"/>
      </a>
    </td>
    <td align="center">
      <a href="https://www.ibm.com/granite">
        <img src="docs/img/ibm-granite.png" width="100" alt="IBM Granite logo"/>
      </a>
    </td>
  </tr>
</table>

It should work for all OpenAI ChatGPT compatible engines, but not verified.
For more information, refer to the [Start guide](docs/start.md).

## Installation
You can install this extension to your Keycloak deployment either using the generated JAR or deploy it as a Quarkus extension.

### Generated JAR

This is common approach on how to include Keycloak extension to Keycloak deployment.
You can just use the generated JAR present in the [GitHub releases]([Latest Release](https://github.com/mabartos/keycloak-adaptive-authn/releases/latest)) assets for this project and put it into the `/providers` folder of your Keycloak installation.

Or you can build your own JAR locally, described in the [Getting started](#getting-started) section below, and put it in the foldewr.

For more details on how to add the JAR to Keycloak installation, see [Installing provider](https://www.keycloak.org/server/configuration-provider#_installing_and_uninstalling_a_provider).

### Quarkus extension

This extensions is made as Quarkus extension and it is possible to include it in Keycloak (powered by Quarkus) distribution as a managed Quarkus extension.
You do not need to take care of transitive dependencies, shaded JARs and problems with your Keycloak JAR extension.

Possible way on how to add the extension to Keycloak installation is via tool [Keycloak Quarkus extensions](https://github.com/mabartos/keycloak-quarkus-extensions).
You can just add the extension to arbitrary version of Keycloak like this:
```shell
./kc-extension.sh add io.github.mabartos:keycloak-adaptive-authn:999.0.0-SNAPSHOT
./kc-extension.sh build
./kc-extension.sh start-dev # access localhost:8080 with the extension present
```

For more details, see the [Add Keycloak Adaptive Authentication extension](https://github.com/mabartos/keycloak-quarkus-extensions/blob/main/examples/keycloak-adaptive-authn.md) guide.

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

2. Start the server with deployed extension

```shell
./mvnw exec:exec@start-server
```

3. Access `localhost:8080/realms/adaptive/account`.

### Container

You can build your own containerized Keycloak installation with this extension as described in this guide: [Add Keycloak Adaptive Authentication extension](https://github.com/mabartos/keycloak-quarkus-extensions/blob/main/examples/keycloak-adaptive-authn.md).

**NOTE**: This is an old release with the authentication policies that are not part of this repository anymore.
Recommended way is to build it from source for now or follow steps on the guide mentioned above.

You can use the container image by running:

    podman run -p 8080:8080 quay.io/mabartos/keycloak-adaptive-all start

This command starts Keycloak exposed on the local port 8080 (`localhost:8080`).

In order to see the functionality in action, navigate to `localhost:8080/realms/authn-policy-adaptive/account`.

ℹ️ **INFO:** If you want to use the OpenAI capabilities, set the environment variables (by setting `-e OPEN_AI_API_*`)
for the image described in the [README](adaptive/README.md#integration-with-openai) of the `adaptive` module..

ℹ️ **INFO:** If you have installed Docker, use `docker` instead of `podman`.

## Connected Authentication Policies

**NOTE**: Authentication policies that were part of this Adaptive authentication initiative were moved to
repository [mabartos/keycloak-authn-policies](https://github.com/mabartos/keycloak-authn-policies).

## Resources with more info

1. ** Unlocking Adaptive Authentication (most recent)**
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
