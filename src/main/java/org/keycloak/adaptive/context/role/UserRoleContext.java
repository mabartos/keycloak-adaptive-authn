package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.models.RoleModel;

import java.util.Set;

public interface UserRoleContext extends UserContext<Set<RoleModel>> {

    @Override
    default boolean requiresUser() {
        return true;
    }

}
