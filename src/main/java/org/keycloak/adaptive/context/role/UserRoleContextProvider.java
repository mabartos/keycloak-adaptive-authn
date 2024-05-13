package org.keycloak.adaptive.context.role;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserRoleContextProvider extends UserRoleContext {
    private final KeycloakSession session;

    public UserRoleContextProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void initData() {
        this.data = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getAuthenticationSession)
                .map(AuthenticationSessionModel::getAuthenticatedUser)
                .map(RoleMapperModel::getRoleMappingsStream)
                .map(f -> f.collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
        this.isInitialized = !data.isEmpty();
    }
}
