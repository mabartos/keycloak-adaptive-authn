package org.keycloak.adaptive.policy;

public class AuthnPolicyNoUserFactory extends AbstractAuthnPoliciesFactory {
    public static final String PROVIDER_IR = "authn-policy-ephemeral-no-user";

    public static final String ALIAS = "Authentication Policies - No user";
    protected static final String DESCRIPTION = "Authentication policies that does not require user";

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public String getAlias() {
        return ALIAS;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public int getPriority() {
        return -999;
    }

    @Override
    public String getId() {
        return PROVIDER_IR;
    }
}
