package io.github.mabartos.context.location;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IpApiLocationContextTest {

    private static final IPAddress TEST_IP = IPAddress.parse("203.0.113.10");

    @Test
    void resolveThroughResolvers_usesSecondResolverWhenFirstReturnsEmpty() {
        LocationData france = LocationDataUtils.create("France", "Paris");
        List<GeoIpResolver> resolvers = List.of(
                stubResolver("first", Optional.empty()),
                stubResolver("second", Optional.of(france)));

        Optional<LocationData> result =
                IpApiLocationContext.resolveThroughResolvers(null, null, TEST_IP, resolvers);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getCountry(), is("France"));
        assertThat(result.get().getCity(), is("Paris"));
    }

    @Test
    void resolveThroughResolvers_returnsEmptyWhenAllResolversFail() {
        List<GeoIpResolver> resolvers = List.of(
                stubResolver("a", Optional.empty()),
                stubResolver("b", Optional.empty()));

        Optional<LocationData> result =
                IpApiLocationContext.resolveThroughResolvers(null, null, TEST_IP, resolvers);

        assertThat(result.isEmpty(), is(true));
        assertThat(GlobalLocationCache.get(TEST_IP).isEmpty(), is(true));
    }

    @Test
    void resolveForIp_returnsEmptyWhenIpAddressMissing() {
        IpApiLocationContext context = new IpApiLocationContext(
                null,
                emptyIpContext(),
                List.of(stubResolver("any", Optional.of(LocationDataUtils.create("France", "Paris")))));

        assertThat(context.resolveForIp(null, null, null).isEmpty(), is(true));
    }

    @Test
    void resolveForIp_returnsEmptyWhenResolversFail() {
        IpApiLocationContext context = new IpApiLocationContext(
                null,
                emptyIpContext(),
                List.of(stubResolver("fail", Optional.empty())));

        assertThat(context.resolveForIp(null, null, TEST_IP).isEmpty(), is(true));
        assertThat(GlobalLocationCache.get(TEST_IP).isEmpty(), is(true));
    }

    private static GeoIpResolver stubResolver(String id, Optional<LocationData> outcome) {
        return new GeoIpResolver() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip) {
                return outcome;
            }
        };
    }

    private static IpAddressContext emptyIpContext() {
        return new IpAddressContext(null) {
            @Override
            public Optional<IPAddress> initData(RealmModel realm) {
                return Optional.empty();
            }
        };
    }
}
