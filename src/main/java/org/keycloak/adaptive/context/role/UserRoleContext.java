package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.models.RoleModel;

import java.util.Set;

public abstract class UserRoleContext extends UserContext<Set<RoleModel>> {

    @Override
    public boolean requiresUser() {
        return true;
    }

}
