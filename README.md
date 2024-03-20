# Keycloak Adaptive Authentication Extension

### Build the project

```shell
mvn clean install -DskipTests
```

### Start Keycloak server with the extension

```shell
mvn exec:exec@start-server
```

#### Execute with specific version of Keycloak (f.e with 24.0.1)

```shell
mvn clean install -DskipTests -Dkeycloak.version=24.0.1
mvn exec:exec@start-server -Dkeycloak.version=24.0.1
```