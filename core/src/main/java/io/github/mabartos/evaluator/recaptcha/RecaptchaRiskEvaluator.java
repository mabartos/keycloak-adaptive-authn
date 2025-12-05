package io.github.mabartos.evaluator.recaptcha;

import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.forms.RecaptchaAssessmentRequest;
import org.keycloak.authentication.forms.RecaptchaAssessmentResponse;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.Set;

import static io.github.mabartos.evaluator.recaptcha.RecaptchaAuthenticatorFactory.SITE_KEY_CONSOLE;

public class RecaptchaRiskEvaluator extends AbstractRiskEvaluator implements Authenticator {
    private static final Logger log = Logger.getLogger(RecaptchaRiskEvaluator.class);

    protected static final String CAPTCHA_TOKEN_KEY = "captcha_token";
    protected static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";

    private final KeycloakSession session;
    private final CloseableHttpClient httpClient;
    private String recaptchaSiteKey;
    private String recaptchaProjectId;
    private String recaptchaProjectApiKey;

    public RecaptchaRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        this.recaptchaSiteKey = RecaptchaAuthenticatorFactory.getSiteKey().orElse("");
        this.recaptchaProjectId = RecaptchaAuthenticatorFactory.getProjectId().orElse("");
        this.recaptchaProjectApiKey = RecaptchaAuthenticatorFactory.getProjectApiKey().orElse("");
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.BEFORE_AUTHN);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.IMPORTANT;
    }

    @Override
    public Risk evaluate() {
        try {
            if (!configIsValid()) {
                return Risk.invalid("Configuration is not valid");
            }

            var token = session.getContext().getAuthenticationSession().getAuthNote(CAPTCHA_TOKEN_KEY);
            if (StringUtil.isBlank(token)) {
                log.warn("No stored reCAPTCHA token. Ignoring this evaluator.");
                return Risk.invalid("Cannot find stored reCAPTCHA token");
            }

            HttpPost request = buildAssessmentRequest(token);
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.errorf("Could not create reCAPTCHA assessment: %s", response.getStatusLine());
                EntityUtils.consumeQuietly(response.getEntity());
                return Risk.invalid("Cannot create reCAPTCHA assessment");
            }

            RecaptchaAssessmentResponse assessment = JsonSerialization.readValue(
                    response.getEntity().getContent(), RecaptchaAssessmentResponse.class);
            log.tracef("Got assessment response: %s", assessment);

            boolean valid = assessment.getTokenProperties().isValid();
            double score = assessment.getRiskAnalysis().getScore();

            if (valid) {
                return Risk.of(1.0 - score);
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }

        return Risk.invalid();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        setAuthenticatorConfig(context);

        if (!configIsValid()) {
            log.warn("Cannot obtain configuration for reCAPTCHA v3 risk evaluator: missing authenticator configuration, or env vars. Ignoring this evaluator.");
            context.success();
            return;
        }

        Response response = context.form()
                .setAttribute("recaptchaSiteKey", recaptchaSiteKey)
                .createForm("recaptcha-risk-evaluator.ftl");
        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        var receivedCaptcha = formData.getFirst(G_RECAPTCHA_RESPONSE);
        context.getAuthenticationSession().setAuthNote(CAPTCHA_TOKEN_KEY, receivedCaptcha);
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

    protected boolean configIsValid() {
        return StringUtil.isNotBlank(recaptchaSiteKey) &&
                StringUtil.isNotBlank(recaptchaProjectId) &&
                StringUtil.isNotBlank(recaptchaProjectApiKey);
    }

    protected void setAuthenticatorConfig(AuthenticationFlowContext context) {
        var authenticatorSiteKey = context.getAuthenticatorConfig().getConfig().get(SITE_KEY_CONSOLE);
        var authenticatorProjectId = context.getAuthenticatorConfig().getConfig().get(SITE_KEY_CONSOLE);
        var authenticatorProjectApiKey = context.getAuthenticatorConfig().getConfig().get(SITE_KEY_CONSOLE);

        if (StringUtil.isNotBlank(authenticatorSiteKey)) {
            recaptchaSiteKey = authenticatorSiteKey;
        }
        if (StringUtil.isNotBlank(authenticatorProjectId)) {
            recaptchaProjectId = authenticatorProjectId;
        }
        if (StringUtil.isNotBlank(authenticatorProjectApiKey)) {
            recaptchaProjectApiKey = authenticatorProjectApiKey;
        }
    }

    protected HttpPost buildAssessmentRequest(String captcha) throws IOException {
        String url = String.format("https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s",
                recaptchaProjectId, recaptchaProjectApiKey);

        HttpPost request = new HttpPost(url);
        RecaptchaAssessmentRequest body = new RecaptchaAssessmentRequest(
                captcha, recaptchaSiteKey, "LOGIN");
        request.setEntity(new StringEntity(JsonSerialization.writeValueAsString(body)));
        request.setHeader("Content-type", "application/json; charset=utf-8");

        log.tracef("Built assessment request: %s", body);
        return request;
    }
}
