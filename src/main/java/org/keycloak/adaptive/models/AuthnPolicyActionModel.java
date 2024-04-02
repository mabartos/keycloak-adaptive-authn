package org.keycloak.adaptive.models;

import org.keycloak.models.AuthenticationExecutionModel;

public class AuthnPolicyActionModel extends AuthenticationExecutionModel {

    public String getActionProvider() {
        return getAuthenticator();
    }

    void setActionProvider(String provider) {
        setAuthenticator(provider);
    }
}
