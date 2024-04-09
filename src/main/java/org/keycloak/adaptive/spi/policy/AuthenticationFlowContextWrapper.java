package org.keycloak.adaptive.spi.policy;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
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
    private final RealmModel realm;
    private final AuthenticationFlowContext delegate;
    private final AuthenticatorConfigModel configModel;

    public AuthenticationFlowContextWrapper(RealmModel realm, AuthenticationFlowContext delegate, String authenticatorConfigId) {
        this.realm = realm;
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
        return null;
    }

    @Override
    public RealmModel getRealm() {
        return null;
    }

    @Override
    public ClientConnection getConnection() {
        return null;
    }

    @Override
    public UriInfo getUriInfo() {
        return null;
    }

    @Override
    public KeycloakSession getSession() {
        return null;
    }

    @Override
    public HttpRequest getHttpRequest() {
        return null;
    }

    @Override
    public BruteForceProtector getProtector() {
        return null;
    }

    @Override
    public FormMessage getForwardedErrorMessage() {
        return null;
    }

    @Override
    public FormMessage getForwardedSuccessMessage() {
        return null;
    }

    @Override
    public FormMessage getForwardedInfoMessage() {
        return null;
    }

    @Override
    public void setForwardedInfoMessage(String message, Object... parameters) {

    }

    @Override
    public String generateAccessCode() {
        return null;
    }

    @Override
    public AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String authenticatorCategory) {
        return null;
    }

    @Override
    public void success() {

    }

    @Override
    public void failure(AuthenticationFlowError error) {

    }

    @Override
    public void failure(AuthenticationFlowError error, Response response) {

    }

    @Override
    public void failure(AuthenticationFlowError error, Response response, String eventDetails, String userErrorMessage) {

    }

    @Override
    public void challenge(Response challenge) {

    }

    @Override
    public void forceChallenge(Response challenge) {

    }

    @Override
    public void failureChallenge(AuthenticationFlowError error, Response challenge) {

    }

    @Override
    public void attempted() {

    }

    @Override
    public FlowStatus getStatus() {
        return null;
    }

    @Override
    public AuthenticationFlowError getError() {
        return null;
    }

    @Override
    public String getEventDetails() {
        return null;
    }

    @Override
    public String getUserErrorMessage() {
        return null;
    }
}
