package org.keycloak.adaptive.context.role;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserRoleContextProvider implements UserRoleContext {
    private final KeycloakSession session;
    private Set<RoleModel> data;
    private boolean isInitialized;

    public UserRoleContextProvider(KeycloakSession session) {
        this.session = session;
        initData();
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void initData() {
        this.data = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getAuthenticationSession)
                .map(AuthenticationSessionModel::getAuthenticatedUser)
                .map(RoleMapperModel::getRoleMappingsStream)
                .map(f -> f.collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
        this.isInitialized = true;
    }

    @Override
    public Set<RoleModel> getData() {
        return data;
    }
}
