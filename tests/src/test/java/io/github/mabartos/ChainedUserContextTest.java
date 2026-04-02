/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.client.DeviceIpAddressContext;
import io.github.mabartos.context.ip.client.HeaderIpAddressContext;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.ip.client.TestIpAddressContext;
import io.github.mabartos.context.location.AuthnSessionLocationContext;
import io.github.mabartos.context.location.GlobalCacheLocationContext;
import io.github.mabartos.context.location.IpApiLocationContext;
import io.github.mabartos.context.location.LocationContext;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the ChainedUserContext that chains through multiple
 * UserContext implementations by priority.
 */
@KeycloakIntegrationTest(config = ChainedUserContextTest.Config.class)
public class ChainedUserContextTest {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    /**
     * Test that UserContexts.getContext() returns a valid context that chains through
     * multiple IpAddressContext implementations by priority.
     * <p>
     * The priority order should be:
     * 1. TestIpAddressContext (priority 999)
     * 2. DeviceIpAddressContext (priority 10)
     * 3. HeaderIpAddressContext (priority 9)
     */
    @Test
    @Order(1)
    public void delegates() {
        runOnServer.run(session -> {
            IpAddressContext context = UserContexts.getContext(session, IpAddressContext.class);
            assertNotNull(context);
            assertThat(context.getClass(), is(TestIpAddressContext.class));

            // Device representation IP delegate
            var firstDelegate = context.getDelegate();
            assertThat(firstDelegate.isPresent(), is(true));
            assertThat(firstDelegate.get().getClass(), is(DeviceIpAddressContext.class));

            // Header IP delegate
            var secondDelegate = firstDelegate.get().getDelegate();
            assertThat(secondDelegate.isPresent(), is(true));
            assertThat(secondDelegate.get().getClass(), is(HeaderIpAddressContext.class));
        });
    }

    @Test
    @Order(2)
    public void chainedContextInitialization() {
        runOnServer.run(session -> {
            IpAddressContext context = UserContexts.getContext(session, IpAddressContext.class);
            assertNotNull(context);
            assertThat(context.getClass(), is(TestIpAddressContext.class));
            assertThat(TestIpAddressContext.isTestIpAddressUsed(), is(false));

            Optional<IPAddress> data = context.getData(null);
            assertTrue(data.isPresent());
            // the TestIP context is disabled, so the IP should be obtained from the DeviceRepresentation
            assertEquals("127.0.0.1", data.get().toString());
        });
    }

    @Test
    @Order(3)
    public void chainedContextProviderNotEnabled() {
        runOnServer.run(session -> {
            System.setProperty(TestIpAddressContext.USE_TESTING_IP_PROP, "true");
            try {
                IpAddressContext context = UserContexts.getContext(session, IpAddressContext.class);
                assertNotNull(context);
                assertThat(context.getClass(), is(TestIpAddressContext.class));
                assertThat(TestIpAddressContext.isTestIpAddressUsed(), is(true));

                Optional<IPAddress> data = context.getData(null);
                assertTrue(data.isPresent());
                // It should return data from the TestIpAddress context
                assertThat(data.get().toString(), is(TestIpAddressContext.TESTING_IP));
            } finally {
                System.clearProperty(TestIpAddressContext.USE_TESTING_IP_PROP);
            }
        });
    }

    @Test
    public void chainedContextThrowsWhenNoProviders() {
        runOnServer.run(session -> {
            assertThrows(IllegalStateException.class, () -> {
                // This should throw because NonExistentContext doesn't have any registered providers
                UserContexts.getContext(session, NonExistentContext.class);
            });
        });
    }

    @Test
    @Order(4)
    public void locationContextDelegatesByCachePriority() {
        runOnServer.run(session -> {
            LocationContext context = UserContexts.getContext(session, LocationContext.class);
            assertNotNull(context);
            assertThat(context.getClass(), is(AuthnSessionLocationContext.class));

            var firstDelegate = context.getDelegate();
            assertThat(firstDelegate.isPresent(), is(true));
            assertThat(firstDelegate.get().getClass(), is(GlobalCacheLocationContext.class));

            var secondDelegate = firstDelegate.get().getDelegate();
            assertThat(secondDelegate.isPresent(), is(true));
            assertThat(secondDelegate.get().getClass(), is(IpApiLocationContext.class));
        });
    }

    // Dummy context class for testing error case
    private static class NonExistentContext extends IpAddressContext {
        public NonExistentContext(KeycloakSession session) {
            super(session);
        }

        @Override
        public Optional<IPAddress> initData(RealmModel realm) {
            return Optional.empty();
        }
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn");
        }
    }
}
