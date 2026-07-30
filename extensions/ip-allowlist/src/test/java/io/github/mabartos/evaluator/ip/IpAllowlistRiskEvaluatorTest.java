package io.github.mabartos.evaluator.ip;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.ip.allowlist.IpAllowlistMatcher;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Optional;

import static io.github.mabartos.spi.level.Risk.Score.INVALID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class IpAllowlistRiskEvaluatorTest {

    @Test
    void allInvalidEntriesReturnsInvalid() {
        var matcher = IpAllowlistMatcher.fromEntries(
                IpAllowlistEvaluatorConfig.splitEntries("not-an-ip,garbage,2001:db8::1,10.0.0.0/99"));
        var evaluator = new IpAllowlistRiskEvaluator(new FixedIpAddressContext(Optional.empty()));

        Risk risk = evaluator.evaluateWithMatcher(matcher, null);

        assertThat(matcher.isEmpty(), is(true));
        assertThat(risk.getScore(), is(INVALID));
        assertThat(risk.getReason().orElse(""), is("IP allowlist is not configured"));
    }

    private static final class FixedIpAddressContext extends IpAddressContext {
        private final Optional<IPAddress> ip;

        FixedIpAddressContext(Optional<IPAddress> ip) {
            super(null);
            this.ip = ip;
        }

        @Override
        public Optional<IPAddress> initData(@Nonnull RealmModel realm) {
            return ip;
        }

        @Override
        public Optional<IPAddress> getData(@Nonnull RealmModel realm) {
            return ip;
        }

        @Override
        public Optional<IPAddress> getData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
            return ip;
        }
    }
}
