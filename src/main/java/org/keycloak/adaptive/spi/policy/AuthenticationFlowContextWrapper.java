package org.keycloak.adaptive.spi.policy;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.List;

public class AuthenticationFlowContextWrapper implements AuthenticationFlowContext {
    private final AuthenticationFlowContext delegate;
    private final AuthenticatorConfigModel configModel;

    public AuthenticationFlowContextWrapper(RealmModel realm, AuthenticationFlowContext delegate, String authenticatorConfigId) {
        this.delegate = delegate;
        this.configModel = realm.getAuthenticatorConfigById(authenticatorConfigId);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfig() {
        return configModel;
    }

    // DELEGATED
    @Override
    public UserModel getUser() {
        return delegate.getUser();
    }

    @Override
    public void setUser(UserModel user) {
        delegate.setUser(user);
    }

    @Override
    public List<AuthenticationSelectionOption> getAuthenticationSelections() {
        return delegate.getAuthenticationSelections();
    }

    @Override
    public void setAuthenticationSelections(List<AuthenticationSelectionOption> credentialAuthExecMap) {
        delegate.setAuthenticationSelections(credentialAuthExecMap);
    }

    @Override
    public void clearUser() {
        delegate.clearUser();
    }

    @Override
    public void attachUserSession(UserSessionModel userSession) {
        delegate.attachUserSession(userSession);
    }

    @Override
    public AuthenticationSessionModel getAuthenticationSession() {
        return delegate.getAuthenticationSession();
    }

    @Override
    public String getFlowPath() {
        return delegate.getFlowPath();
    }

    @Override
    public LoginFormsProvider form() {
        return delegate.form();
    }

    @Override
    public URI getActionUrl(String code) {
        return delegate.getActionUrl(code);
    }

    @Override
    public URI getActionTokenUrl(String tokenString) {
        return delegate.getActionTokenUrl(tokenString);
    }

    @Override
    public URI getRefreshExecutionUrl() {
        return delegate.getRefreshExecutionUrl();
    }

    @Override
    public URI getRefreshUrl(boolean authSessionIdParam) {
        return delegate.getRefreshUrl(authSessionIdParam);
    }

    @Override
    public void cancelLogin() {
        delegate.cancelLogin();
    }

    @Override
    public void resetFlow() {
        delegate.resetFlow();
    }

    @Override
    public void resetFlow(Runnable afterResetListener) {
        delegate.resetFlow(afterResetListener);
    }

    @Override
    public void fork() {
        delegate.fork();
    }

    @Override
    public void forkWithSuccessMessage(FormMessage message) {
        delegate.forkWithSuccessMessage(message);
    }

    @Override
    public void forkWithErrorMessage(FormMessage message) {
        delegate.forkWithErrorMessage(message);
    }

    @Override
    public EventBuilder getEvent() {
        return delegate.getEvent();
    }

    @Override
    public EventBuilder newEvent() {
        return delegate.newEvent();
    }

    @Override
    public AuthenticationExecutionModel getExecution() {
        return delegate.getExecution();
    }

    @Override
    public RealmModel getRealm() {
        return delegate.getRealm();
    }

    @Override
    public ClientConnection getConnection() {
        return delegate.getConnection();
    }

    @Override
    public UriInfo getUriInfo() {
        return delegate.getUriInfo();
    }

    @Override
    public KeycloakSession getSession() {
        return delegate.getSession();
    }

    @Override
    public HttpRequest getHttpRequest() {
        return delegate.getHttpRequest();
    }

    @Override
    public BruteForceProtector getProtector() {
        return delegate.getProtector();
    }

    @Override
    public FormMessage getForwardedErrorMessage() {
        return delegate.getForwardedErrorMessage();
    }

    @Override
    public FormMessage getForwardedSuccessMessage() {
        return delegate.getForwardedSuccessMessage();
    }

    @Override
    public FormMessage getForwardedInfoMessage() {
        return delegate.getForwardedInfoMessage();
    }

    @Override
    public void setForwardedInfoMessage(String message, Object... parameters) {
        delegate.setForwardedInfoMessage(message, parameters);
    }

    @Override
    public String generateAccessCode() {
        return delegate.generateAccessCode();
    }

    @Override
    public AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String authenticatorCategory) {
        return delegate.getCategoryRequirementFromCurrentFlow(authenticatorCategory);
    }

    @Override
    public void success() {
        delegate.success();
    }

    @Override
    public void failure(AuthenticationFlowError error) {
        delegate.failure(error);
    }

    @Override
    public void failure(AuthenticationFlowError error, Response response) {
        delegate.failure(error, response);
    }

    @Override
    public void failure(AuthenticationFlowError error, Response response, String eventDetails, String userErrorMessage) {
        delegate.failure(error, response, eventDetails, userErrorMessage);
    }

    @Override
    public void challenge(Response challenge) {
        delegate.challenge(challenge);
    }

    @Override
    public void forceChallenge(Response challenge) {
        delegate.forceChallenge(challenge);
    }

    @Override
    public void failureChallenge(AuthenticationFlowError error, Response challenge) {
        delegate.failureChallenge(error, challenge);
    }

    @Override
    public void attempted() {
        delegate.attempted();
    }

    @Override
    public FlowStatus getStatus() {
        return delegate.getStatus();
    }

    @Override
    public AuthenticationFlowError getError() {
        return delegate.getError();
    }

    @Override
    public String getEventDetails() {
        return delegate.getEventDetails();
    }

    @Override
    public String getUserErrorMessage() {
        return delegate.getUserErrorMessage();
    }
}
