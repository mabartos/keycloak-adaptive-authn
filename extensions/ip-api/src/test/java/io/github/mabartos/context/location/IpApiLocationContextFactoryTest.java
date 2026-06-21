package io.github.mabartos.context.location;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class IpApiLocationContextFactoryTest {

    @Test
    void buildResolvers_parsesFreeProvidersInOrder() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(
                "ip-api-com-free,ipapi-co-free", null, null);

        assertThat(resolvers, hasSize(2));
        assertThat(resolvers.get(0).id(), is(IpApiComGeoIpResolver.RESOLVER_ID_FREE));
        assertThat(resolvers.get(1).id(), is(IpApiCoGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void buildResolvers_skipsProWithoutCredentials() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(
                "ipapi-co-pro,ip-api-com-pro", "   ", null);

        assertThat(resolvers, hasSize(1));
        assertThat(resolvers.get(0).id(), is(IpApiCoGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void buildResolvers_includesProWhenCredentialsPresent() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(
                "ipapi-co-pro,ip-api-com-pro", "ipapi-token", "ip-api-key");

        assertThat(resolvers, hasSize(2));
        assertThat(resolvers.get(0), instanceOf(IpApiCoGeoIpResolver.class));
        assertThat(resolvers.get(1), instanceOf(IpApiComGeoIpResolver.class));
        assertThat(resolvers.get(0).id(), is(IpApiCoGeoIpResolver.RESOLVER_ID_PRO));
        assertThat(resolvers.get(1).id(), is(IpApiComGeoIpResolver.RESOLVER_ID_PRO));
    }

    @Test
    void buildResolvers_ignoresUnknownIdsAndFallsBackWhenListEmpty() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(
                "unknown-provider,,bad", null, null);

        assertThat(resolvers, hasSize(1));
        assertThat(resolvers.get(0).id(), is(IpApiCoGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void buildResolvers_defaultsToIpApiCoFreeWhenProvidersBlank() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers("  ", null, null);

        assertThat(resolvers, hasSize(1));
        assertThat(resolvers.get(0).id(), is(IpApiCoGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void buildResolvers_normalizesProviderIdsToLowerCase() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(
                "IPAPI-CO-FREE", null, null);

        assertThat(resolvers.stream().map(GeoIpResolver::id).toList(),
                contains(IpApiCoGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void resolveProvidersForMigration_usesProWhenTokenPresentAndProvidersNotExplicit() {
        String resolved = IpApiLocationContextFactory.resolveProvidersForMigration(
                IpApiCoGeoIpResolver.RESOLVER_ID_FREE, false, "legacy-token");

        assertThat(resolved, is(IpApiCoGeoIpResolver.RESOLVER_ID_PRO));
    }

    @Test
    void isProvidersExplicitlyConfigured_trueWhenPropertyPresentIncludingDefaultId() {
        assertThat(
                IpApiLocationContextFactory.isProvidersExplicitlyConfigured(IpApiCoGeoIpResolver.RESOLVER_ID_FREE),
                is(true));
    }

    @Test
    void isProvidersExplicitlyConfigured_falseWhenPropertyAbsent() {
        assertThat(IpApiLocationContextFactory.isProvidersExplicitlyConfigured(null), is(false));
        assertThat(IpApiLocationContextFactory.isProvidersExplicitlyConfigured("  "), is(false));
    }

    @Test
    void resolveProvidersForMigration_keepsExplicitDefaultProviderWhenTokenPresent() {
        String resolved = IpApiLocationContextFactory.resolveProvidersForMigration(
                IpApiCoGeoIpResolver.RESOLVER_ID_FREE, true, "legacy-token");

        assertThat(resolved, is(IpApiCoGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void resolveProvidersForMigration_keepsPropertyChainWhenMultiProviderAndTokenPresent() {
        String configured = IpApiCoGeoIpResolver.RESOLVER_ID_FREE + "," + IpApiComGeoIpResolver.RESOLVER_ID_FREE;
        boolean explicit = IpApiLocationContextFactory.isProvidersExplicitlyConfigured(configured);
        String resolved = IpApiLocationContextFactory.resolveProvidersForMigration(
                configured, explicit, "legacy-token");

        assertThat(explicit, is(true));
        assertThat(resolved, is(configured));
    }

    @Test
    void buildResolvers_skipsDuplicateProviderIds() {
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(
                "ipapi-co-free,ipapi-co-free,ip-api-com-free", null, null);

        assertThat(resolvers, hasSize(2));
        assertThat(resolvers.stream().map(GeoIpResolver::id).toList(),
                contains(IpApiCoGeoIpResolver.RESOLVER_ID_FREE, IpApiComGeoIpResolver.RESOLVER_ID_FREE));
    }

    @Test
    void buildResolvers_migrationTokenWithoutExplicitProvidersUsesPro() {
        String resolved = IpApiLocationContextFactory.resolveProvidersForMigration(null, false, "legacy-token");
        List<GeoIpResolver> resolvers = IpApiLocationContextFactory.buildResolvers(resolved, "legacy-token", null);

        assertThat(resolvers, hasSize(1));
        assertThat(resolvers.get(0).id(), is(IpApiCoGeoIpResolver.RESOLVER_ID_PRO));
    }
}
