# IP API Location Extension

Location context provider for [Keycloak Adaptive Authentication](../../README.md) that resolves geolocation data from client IP via configurable providers ([ipapi.co](https://ipapi.co/), [ip-api.com](https://ip-api.com/)).

## What it does

During authentication, the extension:

1. Retrieves the client's IP address via `IpAddressContext`
2. Tries an ordered chain of GeoIP resolvers until one returns `LocationData`

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
| `KC_ADAPTIVE_LOCATION_PROVIDERS` | Comma-separated GeoIP resolver ids (try order) | No | `ipapi-co-free` |
| `KC_ADAPTIVE_IPAPI_TOKEN` | API token for [ipapi.co](https://ipapi.co/) | For `ipapi-co-pro` | _(none)_ |
| `KC_ADAPTIVE_IP_API_COM_API_KEY` | Pro API key for [ip-api.com](https://ip-api.com) | For `ip-api-com-pro` | _(none)_ |

**Resolver ids:**

| Id | Backend | Notes |
|---|---|---|
| `ipapi-co-free` | ipapi.co | No token |
| `ipapi-co-pro` | ipapi.co | Requires `KC_ADAPTIVE_IPAPI_TOKEN` |
| `ip-api-com-free` | ip-api.com | ⚠️ HTTP only — dev/non-prod |
| `ip-api-com-pro` | ip-api.com | requires `KC_ADAPTIVE_IP_API_COM_API_KEY` |

If every resolver fails, no location is returned (not cached). Location conditions then use `<unknown>` for country/city.

⚠️ **`ip-api-com-free`** uses plain HTTP. Prefer `ip-api-com-pro` or `ipapi-co-*` in production.

**Example (Docker):**

```shell
docker run ... \
  -e KC_ADAPTIVE_LOCATION_PROVIDERS=ip-api-com-pro,ipapi-co-pro \
  -e KC_ADAPTIVE_IPAPI_TOKEN=your_ipapi_token \
  -e KC_ADAPTIVE_IP_API_COM_API_KEY=your_ip_api_com_key ...
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

The extension resolves the following fields from providers :

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
        └── GeoIpResolver chain (from KC_ADAPTIVE_LOCATION_PROVIDERS)
              ├── IpApiCoGeoIpResolver   → ipapi.co
              └── IpApiComGeoIpResolver  → ip-api.com
```

The extension integrates into the location context chain — when multiple `LocationContext` implementations are present (e.g. cache layers), they delegate to each other based on priority.
