# IP API Location Extension

Location context provider for [Keycloak Adaptive Authentication](../../README.md) that resolves geolocation data from client IP via configurable providers ([ipapi.co](https://ipapi.co/), [ip-api.com](https://ip-api.com/), [MaxMind GeoLite2](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data/)).

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

The extension is auto-discovered via Java's `ServiceLoader` mechanism (`META-INF/services` for `UserContextFactory`, `GeoIpResolverFactory`, and `GeoIpResolverSpi`).

## Configuration

### Environment Variables

Configuration is done exclusively through environment variables. Set them on your Keycloak instance before starting the server.

| Environment Variable | Description | Required | Default |
|---|---|---|---|
| `KC_ADAPTIVE_LOCATION_PROVIDERS` | Comma-separated GeoIP resolver ids (try order) | No | `ipapi-co-free` |
| `KC_ADAPTIVE_IPAPI_TOKEN` | API token for [ipapi.co](https://ipapi.co/) | For `ipapi-co-pro` | _(none)_ |
| `KC_ADAPTIVE_IP_API_COM_API_KEY` | Pro API key for [ip-api.com](https://ip-api.com) | For `ip-api-com-pro` | _(none)_ |
| `KC_ADAPTIVE_MAXMIND_ACCOUNT_ID` | MaxMind account ID | For official MaxMind download | _(none)_ |
| `KC_ADAPTIVE_MAXMIND_LICENSE_KEY` | MaxMind license key | For official MaxMind download | _(none)_ |
| `KC_ADAPTIVE_MAXMIND_DB_REFRESH_INTERVAL` | ISO-8601 duration between GeoLite2-City refreshes | No | `PT7D` |
| `KC_ADAPTIVE_MAXMIND_DB_PATH` | Local path for `GeoLite2-City.mmdb` | No | `{kc.data-dir}/adaptive-authn/geolite2/GeoLite2-City.mmdb` |

**Resolver ids:**

| Id | Backend | Notes |
|---|---|---|
| `ipapi-co-free` | ipapi.co | No token |
| `ipapi-co-pro` | ipapi.co | Requires `KC_ADAPTIVE_IPAPI_TOKEN` |
| `ip-api-com-free` | ip-api.com | ⚠️ HTTP only — dev/non-prod |
| `ip-api-com-pro` | ip-api.com | requires `KC_ADAPTIVE_IP_API_COM_API_KEY` |
| `maxmind` | GeoLite2-City (local MMDB) | No per-lookup HTTP; see MaxMind section below |

If every resolver fails, no location is returned (not cached). Location conditions then use `<unknown>` for country/city.

⚠️ **`ip-api-com-free`** uses plain HTTP. Prefer `ip-api-com-pro` or `ipapi-co-*` in production.

**Example (Docker, HTTP providers):**

```shell
docker run ... \
  -e KC_ADAPTIVE_LOCATION_PROVIDERS=ip-api-com-pro,ipapi-co-pro \
  -e KC_ADAPTIVE_IPAPI_TOKEN=your_ipapi_token \
  -e KC_ADAPTIVE_IP_API_COM_API_KEY=your_ip_api_com_key ...
```

**Example (MaxMind, production):**

```shell
docker run ... \
  -e KC_ADAPTIVE_LOCATION_PROVIDERS=maxmind \
  -e KC_ADAPTIVE_MAXMIND_ACCOUNT_ID=123456 \
  -e KC_ADAPTIVE_MAXMIND_LICENSE_KEY=your_license_key
```

**Example (MaxMind, POC without MaxMind account):**

```shell
docker run ... \
  -e KC_ADAPTIVE_LOCATION_PROVIDERS=maxmind
```

Without MaxMind credentials, the extension downloads GeoLite2-City from the [P3TERX community mirror](https://github.com/P3TERX/GeoLite.mmdb) at startup. This is convenient for quick testing but **not** compliant with the [GeoLite2 EULA](https://www.maxmind.com/en/geolite2/eula) for production; use official credentials in prod.

**Example (mixed fallback):**

```shell
docker run ... \
  -e KC_ADAPTIVE_LOCATION_PROVIDERS=maxmind,ipapi-co-free
```

### MaxMind database lifecycle

When `maxmind` is listed in `KC_ADAPTIVE_LOCATION_PROVIDERS`:

1. On Keycloak startup, the extension checks for a local `GeoLite2-City.mmdb` at `KC_ADAPTIVE_MAXMIND_DB_PATH` (or the default under `kc.data-dir`).
2. If the file is missing or older than `KC_ADAPTIVE_MAXMIND_DB_REFRESH_INTERVAL`, it downloads a fresh copy:
   - **Official** (both `KC_ADAPTIVE_MAXMIND_ACCOUNT_ID` and `KC_ADAPTIVE_MAXMIND_LICENSE_KEY` set): from `download.maxmind.com`
   - **Mirror** (no credentials): from the P3TERX GitHub repository
3. A background task re-checks and refreshes the database on the configured interval.

GeoLite2-City is updated by MaxMind twice weekly (Tuesday and Friday). The default refresh interval of 7 days keeps the database current within the EULA's 30-day requirement.

Lookups are performed locally via the [GeoIP2 Java API](https://github.com/maxmind/GeoIP2-java); no external HTTP call is made per authentication request.

### Note on configuration

The extension reads configuration through SmallRye Config (Keycloak's Quarkus `Configuration` API), which automatically resolves environment variables — no `application.properties` file is needed.

Property keys follow Keycloak's kebab-case form (as in `kc.sh show-config`), e.g. `KC_ADAPTIVE_IP_API_COM_API_KEY` → `kc.adaptive-ip-api-com-api-key`.

### Related core configuration

The core module provides additional location-related settings that affect all location context providers, including this one:

| Environment Variable | Description | Default |
|---|---|---|
| `KC_ADAPTIVE_LOCATION_GLOBAL_CACHE_TTL` | TTL for the global location cache | `PT24H` (24 hours) |
| `KC_ADAPTIVE_LOCATION_GLOBAL_CACHE_MAXIMUM_SIZE` | Max entries in the global location cache | `10000` |
| `KC_ADAPTIVE_TESTING_RANDOM_IP_ENABLED` | Use random IPs for testing | `false` |
| `KC_ADAPTIVE_TESTING_IP_VALUE` | Override IP address for testing | _(none)_ |

## Location data

The extension resolves the following fields from providers:

| Field | Example | MaxMind (`maxmind`) |
|---|---|---|
| City | `Prague` | ✅ |
| Region | `Hlavni mesto Praha` | ✅ |
| Region code | `10` | ✅ |
| Country | `Czechia` | ✅ |
| Continent | `EU` | ✅ |
| Postal code | `110 00` | ✅ (when available) |
| Latitude | `50.0833` | ✅ |
| Longitude | `14.4167` | ✅ |
| Timezone | `Europe/Prague` | ✅ |
| Currency | `CZK` | ❌ (not in GeoLite2-City) |

## Architecture

```
IpApiLocationContextFactory (UserContext SPI)
  └── IpApiLocationContext (remote context)
        ├── Uses IpAddressContext to get client IP
        └── GeoIpResolverChain (order from KC_ADAPTIVE_LOCATION_PROVIDERS)
              └── GeoIpResolver SPI providers (registered at build; pro tiers gated at runtime on credentials)
                    ├── ipapi-co-free / ipapi-co-pro      → IpApiCoGeoIpResolver (HTTP)
                    ├── ip-api-com-free / ip-api-com-pro  → IpApiComGeoIpResolver (HTTP)
                    └── maxmind                           → MaxMindGeoIpResolver (local MMDB)
                              └── MaxMindDatabaseManager (download + scheduled refresh)
```

Each backend is a separate `GeoIpResolverFactory` in this extension JAR. Pro HTTP tiers are skipped at runtime when their credential env var is unset (factories stay registered so a Keycloak rebuild is not required when secrets are supplied only at container start). The MaxMind resolver works without credentials (community mirror) or with official MaxMind credentials for production downloads.

## Community maintainer
- [Thomas DELORGE](https://github.com/thomasdelorge)
