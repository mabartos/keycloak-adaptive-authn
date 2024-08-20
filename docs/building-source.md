## Build the project

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

### Export Keycloak Adaptive realm

It will export realm with data for the adaptive authentication

```shell
./mvnw exec:exec@export-realm
```

### Common process of full execution

```shell
./mvnw exec:exec@compile exec:exec@start-server
```

#### Execute with specific version of Keycloak (f.e with 24.0.1)

```shell
./mvnw clean install -DskipTests -Dkeycloak.version=24.0.1
./mvnw exec:exec@start-server -Dkeycloak.version=24.0.1
```