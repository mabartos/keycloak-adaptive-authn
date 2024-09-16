## Build the project

To build the whole project, execute this command:

```shell
./mvnw clean install -DskipTests
```

**WARN:** To properly build the `authn-policy` module, check the particular [README](../authn-policy/README.md).

## Start server with `adaptive` and `authn-policy` modules

Refer to the `common` module to see how to start server with both modules enabled.
You will be able to leverage authentication policies together with the adaptive capabilities.