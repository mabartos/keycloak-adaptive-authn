# Authentication Policies module

This module contains components for Authentication policies.
It is necessary to have a custom Keycloak distribution with the JS changes as we are not able to do it via SPI yet.

The `src` dir contains:

* `js` - TS files for Admin console UI
* `main` - REST API for authentication policies to communicate with the Admin console, authenticators

## Get custom Keycloak distribution

### Get pre-built custom distribution
As the custom build of Keycloak is required, it is necessary to set up your GitHub credentials to obtain already built distribution.

The current supported pre-built version:
- Keycloak **24.0.4**

Provide your PAT in your Maven `settings.xml`, that might look like this:

```xml

<settings>
    <activeProfiles>
        <activeProfile>github</activeProfile>
    </activeProfiles>

    <profiles>
        <profile>
            <id>github</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>https://repo1.maven.org/maven2</url>
                </repository>
                <repository>
                    <id>github</id>
                    <url>https://maven.pkg.github.com/mabartos/keycloak</url>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
        </profile>
    </profiles>

    <servers>
        <server>
            <id>github</id>
            <username>YOUR-USERNAME</username>  <!-- GH username -->
            <password>YOUR_PAT</password>       <!-- GH PAT -->
        </server>
    </servers>
</settings>
```

Or create a different M2 settings (f.e. `my-settings.xml`) and then reference it during the build as:

```shell
mvn clean install -DskipTests -s ~/.m2/settings/my-settings.xml
```

### Build your own Keycloak distribution
It is necessary to build your own Keycloak distribution with the JS changes as we are not able to do it via SPI yet.

Copy the whole `js` folder to the Keycloak distribution, and then set the Keycloak version to `999.0.0-AUTHN-POLICIES` as follows:
```shell
./set-version 999.0.0-AUTHN-POLICIES
```

And then build the whole Keycloak as follows:
```shell
mvn clean install -DskipTests -Dkeycloak.version=999.0.0
```

Or follow instructions in the [Keycloak Building guide](https://github.com/keycloak/keycloak/blob/main/docs/building.md).

## Start the distribution with the extension

You can simply execute this command in this module and the custom Keycloak distribution with this extension will start:
```shell
../mvnw exec:exec@start-server
```

## Show example flow

In order to see the execution of the authentication flow from the example realm `authn-policy`, just access the url `http://localhost:8080/admin/authn-policy/console/`.