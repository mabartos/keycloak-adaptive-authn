package io.github.mabartos.evaluator.device;

import io.github.mabartos.context.device.KnownDeviceConstants;
import io.github.mabartos.context.device.VisitorIdUtils;
import io.github.mabartos.evaluator.EvaluatorUtils;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

/**
 * Collects a FingerprintJS visitor identifier during the browser login flow.
 * The identifier is stored in the authentication session for {@link KnownDeviceRiskEvaluator}.
 */
public class DeviceFingerprintCollector implements Authenticator {
    private static final Logger log = Logger.getLogger(DeviceFingerprintCollector.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (!isFingerprintEvaluatorEnabled(context.getRealm())) {
            log.debug("Known device evaluator disabled, skipping fingerprint collection");
            context.success();
            return;
        }

        Response response = context.form()
                .createForm("device-fingerprint-collector.ftl");
        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String visitorId = formData.getFirst(KnownDeviceConstants.FORM_PARAM);

        if (VisitorIdUtils.isValidVisitorId(visitorId)) {
            context.getAuthenticationSession().setAuthNote(
                    KnownDeviceConstants.VISITOR_ID_AUTH_NOTE, visitorId);
            log.tracef("Stored device visitor id in auth note");
        } else {
            context.getAuthenticationSession().removeAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE);
            if (StringUtil.isNotBlank(visitorId)) {
                log.debugf("Ignoring invalid device visitor id format");
            } else {
                log.debug("No device visitor id submitted (blocked JS or collection failure)");
            }
        }

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

    private boolean isFingerprintEvaluatorEnabled(RealmModel realm) {
        return EvaluatorUtils.isEvaluatorEnabled(realm, KnownDeviceRiskEvaluator.class);
    }
}
