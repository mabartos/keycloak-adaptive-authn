package io.github.mabartos.evaluator.ip;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.ip.whitelist.IpWhitelistMatcher;
import io.github.mabartos.spi.evaluator.DeviceRiskEvaluator;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;

/**
 * Reduces risk when the client IP matches a configured IPv4 whitelist.
 */
@EvaluationPhase(BEFORE_AUTHN)
public class IpWhitelistRiskEvaluator extends DeviceRiskEvaluator {

    private final IpAddressContext ipAddressContext;

    public IpWhitelistRiskEvaluator(KeycloakSession session) {
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    IpWhitelistRiskEvaluator(IpAddressContext ipAddressContext) {
        this.ipAddressContext = ipAddressContext;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm) {
        return evaluateWithMatcher(whitelistMatcher(realm).orElse(null), realm);
    }

    /**
     * Evaluates risk for a pre-resolved matcher. Used by {@link #evaluate(RealmModel)} and unit tests
     * that exercise matcher/config parsing without a live Keycloak realm.
     */
    Risk evaluateWithMatcher(IpWhitelistMatcher matcher, RealmModel realm) {
        if (matcher == null || matcher.isEmpty()) {
            return Risk.invalid("IP whitelist is not configured");
        }

        IPAddress ip = ipAddressContext.getData(realm).orElse(null);
        if (ip == null) {
            return Risk.invalid("Cannot obtain IP address");
        }

        var whitelistedScore = IpWhitelistEvaluatorConfig.whitelistedScore(realm);
        var notWhitelistedScore = IpWhitelistEvaluatorConfig.notWhitelistedScore(realm);

        if (matcher.contains(ip)) {
            return Risk.of(whitelistedScore, "Whitelisted IP address");
        }
        return Risk.of(notWhitelistedScore, "IP address is not whitelisted");
    }

    private static Optional<IpWhitelistMatcher> whitelistMatcher(RealmModel realm) {
        return IpWhitelistMatcherCache.get(realm);
    }
}
