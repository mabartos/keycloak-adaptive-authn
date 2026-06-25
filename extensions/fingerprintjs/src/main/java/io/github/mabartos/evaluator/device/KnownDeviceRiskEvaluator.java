package io.github.mabartos.evaluator.device;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.KnownDeviceConstants;
import io.github.mabartos.context.device.KnownDeviceContext;
import io.github.mabartos.context.device.KnownDeviceData;
import io.github.mabartos.context.device.VisitorIdUtils;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;

/**
 * Evaluates whether the current browser device fingerprint is known for the user.
 * <p>
 * Scoring (aligned with {@code KnownLocationRiskEvaluator}):
 * <ul>
 *   <li>Known active device → {@link Risk.Score#NEGATIVE_LOW}</li>
 *   <li>First tracked device (no active devices in profile) → {@link Risk.Score#VERY_SMALL}</li>
 *   <li>Unknown device (other active devices exist) → {@link Risk.Score#MEDIUM}</li>
 *   <li>Fingerprint missing or invalid, no device history → {@link Risk.Score#VERY_SMALL} (fail-open)</li>
 *   <li>Fingerprint missing or invalid, has device history → {@link Risk.Score#MEDIUM}</li>
 * </ul>
 */
@EvaluationPhase(USER_KNOWN)
public class KnownDeviceRiskEvaluator extends AbstractRiskEvaluator {

    private final KeycloakSession session;
    private final KnownDeviceContext knownDeviceContext;

    public KnownDeviceRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.knownDeviceContext = UserContexts.getContext(session, KnownDeviceContext.class);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel user) {
        if (user == null) {
            return Risk.invalid("User is null");
        }

        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
        if (authSession == null) {
            return Risk.invalid("No authentication session");
        }

        var activeDevices = knownDeviceContext.getData(realm, user).orElse(Set.of());
        boolean hasActiveDevices = !activeDevices.isEmpty();

        String visitorId = authSession.getAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE);
        if (!VisitorIdUtils.isValidVisitorId(visitorId)) {
            if (!hasActiveDevices) {
                return Risk.of(VERY_SMALL, "First tracked device (fingerprint unavailable)");
            }
            return Risk.of(MEDIUM, "Device fingerprint unavailable - treated as unknown device");
        }

        boolean activeKnownDevice = activeDevices.stream()
                .map(KnownDeviceData::visitorId)
                .anyMatch(visitorId::equals);
        if (activeKnownDevice) {
            return Risk.of(NEGATIVE_LOW, "Known device - trust signal");
        }

        if (!hasActiveDevices) {
            return Risk.of(VERY_SMALL, "First tracked device");
        }

        return Risk.of(MEDIUM, "Unknown device for this account");
    }
}
