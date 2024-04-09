# Keycloak Adaptive Authentication Extension

### Build the project

```shell
mvn clean install -DskipTests
```

### Import Keycloak Adaptive realm

It will import realm with data for the adaptive authentication

```shell
mvn exec:exec@import-realm
```

### Start Keycloak server with the extension

```shell
mvn exec:exec@start-server
```

### Common process of full execution

```shell
mvn clean install -DskipTests && mvn exec:exec@import-realm exec:exec@start-server
```

#### Execute with specific version of Keycloak (f.e with 24.0.1)

```shell
mvn clean install -DskipTests -Dkeycloak.version=24.0.1
mvn exec:exec@start-server -Dkeycloak.version=24.0.1
```