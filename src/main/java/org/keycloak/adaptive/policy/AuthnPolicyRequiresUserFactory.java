package org.keycloak.adaptive.policy;

public class AuthnPolicyRequiresUserFactory extends AbstractAuthnPoliciesFactory {
    public static final String PROVIDER_IR = "authn-policy-ephemeral-requires-user";

    public static final String ALIAS = "Authentication Policies - Requires user";
    protected static final String DESCRIPTION = "Authentication policies that requires user";

    @Override
    public boolean requiresUser() {
        return true;
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
        return 999;
    }

    @Override
    public String getId() {
        return PROVIDER_IR;
    }
}
