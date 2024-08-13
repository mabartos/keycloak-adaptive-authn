# Authentication Policies module

This module contains components for Authentication policies.

The `src` dir contains:

* `js` - TS files for Admin console UI
* `main` - REST API for authentication policies to communicate with the Admin console, authenticators

## Build your own Keycloak distribution
It is necessary to build your own Keycloak distribution with the JS changes as we are not able to do it via SPI yet.

Copy the whole `js` folder to the Keycloak distribution, and then set the Keycloak version to `888.0.0-ADAPTIVE` as follows:
```shell
./set-version 888.0.0-ADAPTIVE
```

And then build the whole Keycloak as follows:
```shell
mvn clean install -DskipTests
```

Or follow instructions in the [Keycloak Building guide](https://github.com/keycloak/keycloak/blob/main/docs/building.md).

## Start the distribution with the extension

You can simply execute this command in this module and the custom Keycloak distribution with this extension will start:
```shell
../mvnw exec:exec@start-server
```


