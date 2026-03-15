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
package io.github.mabartos.engine;

import org.keycloak.Config;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskEngineFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Creates risk engine with the virtual threads support
 * <p>
 * This factory requires Java 21+ with --enable-preview flag, or Java 23+ without the flag.
 * The StructuredTaskScope API used by DefaultVTRiskEngine is a preview feature in Java 21-22
 * and becomes stable in Java 23+.
 */
public class DefaultVTRiskEngineFactory implements RiskEngineFactory, EnvironmentDependentProviderFactory {
    public static final String PROVIDER_ID = "default-vt";

    @Override
    public RiskEngine create(KeycloakSession session) {
        return new DefaultVTRiskEngine(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return 10;
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
    public boolean isSupported(Config.Scope config) {
        try {
            Runtime.Version javaVersion = Runtime.version();
            int majorVersion = javaVersion.feature();

            // StructuredTaskScope requires Java 21+ with --enable-preview, or Java 23+ without it
            if (majorVersion < 21) {
                return false;
            }

            // For Java 21-22, check if preview features are enabled
            if (majorVersion < 23) {
                // Check if StructuredTaskScope class is available (preview feature in Java 21-22)
                // This will only be available if --enable-preview flag is set
                try {
                    Class.forName("java.util.concurrent.StructuredTaskScope");
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
