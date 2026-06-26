package io.github.mabartos.context.location.geoip;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.location.LocationData;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class GeoIpResolverChainTest {

    @Test
    void buildChain_parsesFreeProvidersInOrder() {
        Map<String, GeoIpResolver> providers = Map.of(
                GeoIpResolverIds.IP_API_COM_FREE, resolver(GeoIpResolverIds.IP_API_COM_FREE),
                GeoIpResolverIds.IPAPI_CO_FREE, resolver(GeoIpResolverIds.IPAPI_CO_FREE));

        List<GeoIpResolver> chain = GeoIpResolverChain.buildChain(
                List.of(GeoIpResolverIds.IP_API_COM_FREE, GeoIpResolverIds.IPAPI_CO_FREE),
                providers::get);

        assertThat(chain, hasSize(2));
        assertThat(chain.get(0).id(), is(GeoIpResolverIds.IP_API_COM_FREE));
        assertThat(chain.get(1).id(), is(GeoIpResolverIds.IPAPI_CO_FREE));
    }

    @Test
    void buildChain_skipsProWhenProviderUnavailable() {
        Map<String, GeoIpResolver> providers = Map.of(
                GeoIpResolverIds.IPAPI_CO_FREE, resolver(GeoIpResolverIds.IPAPI_CO_FREE));

        List<GeoIpResolver> chain = GeoIpResolverChain.buildChain(
                List.of(GeoIpResolverIds.IPAPI_CO_PRO, GeoIpResolverIds.IP_API_COM_PRO),
                providers::get);

        assertThat(chain, hasSize(1));
        assertThat(chain.get(0).id(), is(GeoIpResolverIds.IPAPI_CO_FREE));
    }

    @Test
    void buildChain_fallsBackWhenListEmpty() {
        Map<String, GeoIpResolver> providers = Map.of(
                GeoIpResolverIds.IPAPI_CO_FREE, resolver(GeoIpResolverIds.IPAPI_CO_FREE));

        List<GeoIpResolver> chain = GeoIpResolverChain.buildChain(
                List.of("unknown-provider", "bad"),
                providers::get);

        assertThat(chain, hasSize(1));
        assertThat(chain.get(0).id(), is(GeoIpResolverIds.IPAPI_CO_FREE));
    }

    @Test
    void parseOrderedProviderIds_defaultsToIpApiCoFreeWhenBlank() {
        assertThat(
                GeoIpResolverChain.parseOrderedProviderIds("  "),
                contains(GeoIpResolverIds.IPAPI_CO_FREE));
    }

    @Test
    void parseOrderedProviderIds_normalizesToLowerCase() {
        assertThat(
                GeoIpResolverChain.parseOrderedProviderIds("IPAPI-CO-FREE"),
                contains(GeoIpResolverIds.IPAPI_CO_FREE));
    }

    @Test
    void resolveProvidersForMigration_usesProWhenTokenPresentAndProvidersNotExplicit() {
        String resolved = GeoIpResolverChain.resolveProvidersForMigration(
                GeoIpResolverIds.IPAPI_CO_FREE, false, "legacy-token");

        assertThat(resolved, is(GeoIpResolverIds.IPAPI_CO_PRO));
    }

    @Test
    void isProvidersExplicitlyConfigured_trueWhenPropertyPresentIncludingDefaultId() {
        assertThat(
                GeoIpResolverChain.isProvidersExplicitlyConfigured(GeoIpResolverIds.IPAPI_CO_FREE),
                is(true));
    }

    @Test
    void isProvidersExplicitlyConfigured_falseWhenPropertyAbsent() {
        assertThat(GeoIpResolverChain.isProvidersExplicitlyConfigured(null), is(false));
        assertThat(GeoIpResolverChain.isProvidersExplicitlyConfigured("  "), is(false));
    }

    @Test
    void resolveProvidersForMigration_keepsExplicitDefaultProviderWhenTokenPresent() {
        String resolved = GeoIpResolverChain.resolveProvidersForMigration(
                GeoIpResolverIds.IPAPI_CO_FREE, true, "legacy-token");

        assertThat(resolved, is(GeoIpResolverIds.IPAPI_CO_FREE));
    }

    @Test
    void resolveProvidersForMigration_keepsPropertyChainWhenMultiProviderAndTokenPresent() {
        String configured = GeoIpResolverIds.IPAPI_CO_FREE + "," + GeoIpResolverIds.IP_API_COM_FREE;
        boolean explicit = GeoIpResolverChain.isProvidersExplicitlyConfigured(configured);
        String resolved = GeoIpResolverChain.resolveProvidersForMigration(
                configured, explicit, "legacy-token");

        assertThat(explicit, is(true));
        assertThat(resolved, is(configured));
    }

    @Test
    void buildChain_skipsDuplicateProviderIds() {
        Map<String, GeoIpResolver> providers = new HashMap<>();
        providers.put(GeoIpResolverIds.IPAPI_CO_FREE, resolver(GeoIpResolverIds.IPAPI_CO_FREE));
        providers.put(GeoIpResolverIds.IP_API_COM_FREE, resolver(GeoIpResolverIds.IP_API_COM_FREE));

        List<GeoIpResolver> chain = GeoIpResolverChain.buildChain(
                List.of(
                        GeoIpResolverIds.IPAPI_CO_FREE,
                        GeoIpResolverIds.IPAPI_CO_FREE,
                        GeoIpResolverIds.IP_API_COM_FREE),
                providers::get);

        assertThat(chain, hasSize(2));
        assertThat(
                chain.stream().map(GeoIpResolver::id).toList(),
                contains(GeoIpResolverIds.IPAPI_CO_FREE, GeoIpResolverIds.IP_API_COM_FREE));
    }

    @Test
    void buildChain_migrationTokenWithoutExplicitProvidersUsesProId() {
        String resolved = GeoIpResolverChain.resolveProvidersForMigration(null, false, "legacy-token");
        List<String> ids = GeoIpResolverChain.parseOrderedProviderIds(resolved);

        assertThat(ids, contains(GeoIpResolverIds.IPAPI_CO_PRO));
    }

    private static GeoIpResolver resolver(String id) {
        return new GeoIpResolver() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip) {
                return Optional.empty();
            }
        };
    }
}
