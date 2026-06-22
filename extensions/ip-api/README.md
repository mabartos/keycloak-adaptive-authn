# IP API Location Extension

Location context provider for [Keycloak Adaptive Authentication](../../README.md) that resolves geolocation data from client IP addresses using the [ipapi.co](https://ipapi.co/) service.

## What it does

During authentication, the extension:

1. Retrieves the client's IP address via `IpAddressContext`
2. Queries `https://ipapi.co/{ip}/json` to resolve geolocation
3. Returns a `LocationData` object with city, region, country, coordinates, timezone, and more

The resolved location data is then available to risk evaluators for adaptive authentication decisions (e.g. detecting logins from unusual locations).

## Installation

1. Build the project:

    ```shell
    mvn clean install -DskipTests
    ```

2. Copy the built JAR (`extensions/ip-api/target/keycloak-adaptive-ext-ip-api-*.jar`) along with the core module JAR to your Keycloak's [`providers/`](https://www.keycloak.org/server/configuration-provider#_installing_and_uninstalling_a_provider) directory.

3. Rebuild Keycloak to pick up the new providers:

    ```shell
    ${KEYCLOAK_HOME}/bin/kc.sh build
    ```

The extension is auto-discovered via Java's `ServiceLoader` mechanism (registered in `META-INF/services`).

## Configuration

### Environment Variables

Configuration is done exclusively through environment variables. Set them on your Keycloak instance before starting the server.

| Environment Variable | Description | Required | Default |
|---|---|---|---|
| `KC_ADAPTIVE_IPAPI_TOKEN` | API token for [ipapi.co](https://ipapi.co/) | No | _(none)_ |

The API token is optional — ipapi.co allows a limited number of unauthenticated requests per day.
For production usage, obtain an API token from [ipapi.co](https://ipapi.co/pricing/) to increase rate limits.

**Example (Docker):**

```shell
docker run ... -e KC_ADAPTIVE_IPAPI_TOKEN=your_token_here ...
```

### Note on configuration

The extension reads configuration through SmallRye Config (Keycloak's Quarkus `Configuration` API), which automatically resolves environment variables — no `application.properties` file is needed.

If you previously used `application.properties` to set `location.ipapi.token`, replace it with the `KC_ADAPTIVE_IPAPI_TOKEN` environment variable instead.

### Related core configuration

The core module provides additional location-related settings that affect all location context providers, including this one:

| Environment Variable | Description | Default |
|---|---|---|
| `KC_ADAPTIVE_LOCATION_GLOBAL_CACHE_TTL` | TTL for the global location cache | `PT24H` (24 hours) |
| `KC_ADAPTIVE_LOCATION_GLOBAL_CACHE_MAXIMUM_SIZE` | Max entries in the global location cache | `10000` |
| `KC_ADAPTIVE_TESTING_RANDOM_IP_ENABLED` | Use random IPs for testing | `false` |
| `KC_ADAPTIVE_TESTING_IP_VALUE` | Override IP address for testing | _(none)_ |

## Location data

The extension resolves the following fields from the ipapi.co response:

| Field | Example |
|---|---|
| City | `Prague` |
| Region | `Hlavni mesto Praha` |
| Region code | `10` |
| Country | `Czechia` |
| Continent | `EU` |
| Postal code | `110 00` |
| Latitude | `50.0833` |
| Longitude | `14.4167` |
| Timezone | `Europe/Prague` |
| Currency | `CZK` |

## Architecture

```
IpApiLocationContextFactory (SPI entry point)
  └── IpApiLocationContext (remote context)
        ├── Uses IpAddressContext to get client IP
        ├── Queries ipapi.co HTTP API
        └── Deserializes response into IpApiLocationData
```

The extension integrates into the location context chain — when multiple `LocationContext` implementations are present (e.g. cache layers), they delegate to each other based on priority.
