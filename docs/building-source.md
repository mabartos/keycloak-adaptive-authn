## Build the project

Before building the whole extension, you need to rebuild Keycloak itself with
branch [adaptive](https://github.com/mabartos/keycloak/tree/adaptive-authn) with version `888.0.0-SNAPSHOT`.
To experiment with the approach, the recommended way is to run the container image for now.

```shell
./mvnw clean install -DskipTests
```

or

```shell
./mvnw exec:exec@compile
```

### Start Keycloak server with the extension

```shell
./mvnw exec:exec@start-server
```

### Import Keycloak Adaptive realm

It will import realm with data for the adaptive authentication

```shell
./mvnw exec:exec@import-realm
```

### Export Keycloak Adaptive realm

It will export realm with data for the adaptive authentication

```shell
./mvnw exec:exec@export-realm
```

### Common process of full execution

```shell
./mvnw exec:exec@compile exec:exec@import-realm exec:exec@start-server
```

#### Execute with specific version of Keycloak (f.e with 24.0.1)

```shell
./mvnw clean install -DskipTests -Dkeycloak.version=24.0.1
./mvnw exec:exec@start-server -Dkeycloak.version=24.0.1
```