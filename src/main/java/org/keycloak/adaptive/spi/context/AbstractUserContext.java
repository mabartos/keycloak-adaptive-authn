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
package org.keycloak.adaptive.spi.context;

import org.keycloak.models.KeycloakSession;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.util.Optional;

public abstract class AbstractUserContext<T> implements UserContext<T> {
    protected static int COUNT_OF_INIT_RETRIES = 2;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<T> data;

    public abstract KeycloakSession getSession();

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isInitialized() {
        return data != null && data.isPresent();
    }

    @Override
    public abstract Optional<T> initData();

    /**
     * Get the specific user context data.
     * If the data are not properly initialized, it retries the retrieval
     *
     * @return specific data
     */
    @Override
    public Optional<T> getData() {
        final var tracingProvider = TracingProviderUtil.getTracingProvider(getSession());

        return tracingProvider.trace(this.getClass(), "getData", (span) -> {
            if (span.isRecording()) {
                span.setAttribute("keycloak.user.context.always-fetch", alwaysFetch());
            }

            if (!alwaysFetch() && isInitialized()) {
                return data;
            }
            this.data = tryInitDataMultipleTimes(tracingProvider);
            return data;
        });
    }

    protected Optional<T> tryInitDataMultipleTimes(TracingProvider tracing) {
        for (int i = 0; i < COUNT_OF_INIT_RETRIES; i++) {
            Optional<T> data = tracing.trace(this.getClass(), "initData", (span) -> {
                return initData();
            });
            if (data.isPresent()) {
                return data;
            }
        }
        return Optional.empty();

    }

    @Override
    public void close() {
    }
}
