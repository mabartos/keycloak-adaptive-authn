package org.keycloak.adaptive.policy;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;

import java.util.List;

public class AuthPoliciesUiPage implements UiPageProvider, UiPageProviderFactory<AuthPoliciesUiPage> {
    private KeycloakSession session;
    private ComponentModel model;

    public AuthPoliciesUiPage() {
    }

    @Override
    public AuthPoliciesUiPage create(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "Authentication Policies";
    }

    @Override
    public String getHelpText() {
        return "Here you can store your Todo items";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("name")
                .label("Name")
                .helpText("Short name of the task")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().property()
                .name("description")
                .label("Description")
                .helpText("Description of what needs to be done")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .add().property()
                .name("prio")
                .label("Priority")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options("critical", "high priority", "neutral", "low priority", "unknown")
                .add().build();
    }
}
