package org.keycloak.adaptive.context.ip.client;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.models.KeycloakSession;

import java.util.List;

public class DefaultIpAddress extends IpAddressContext {
    private final KeycloakSession session;
    private final List<IpAddressContext> contexts;

    public DefaultIpAddress(KeycloakSession session) {
        this.session = session;
        this.contexts = ContextUtils.getSortedContexts(session, IpAddressContext.class);
    }

    @Override
    public void initData() {
        for (var context : contexts) {
            context.initData();
            if (context.isInitialized()) {
                this.data = context.getData();
                this.isInitialized = true;
                return;
            }
        }
        this.isInitialized = false;
    }
}
