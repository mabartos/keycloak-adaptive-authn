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
package io.github.mabartos.spi.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractUserContext<T> implements UserContext<T> {
    protected static int COUNT_OF_INIT_RETRIES = 2;

    protected final KeycloakSession session;
    private final ReentrantLock lock = new ReentrantLock();

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<T> data;
    private UserContext<T> delegate;

    public AbstractUserContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isInitialized() {
        return data != null && data.isPresent();
    }

    /**
     * Get the specific user context data.
     * If the data are not properly initialized, it retries the retrieval.
     * Thread-safe lazy initialization using ReentrantLock (compatible with virtual threads in JDK 21+)
     *
     * @return specific data
     */
    @Override
    public Optional<T> getData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        final var tracingProvider = TracingProviderUtil.getTracingProvider(session);

        return tracingProvider.trace(this.getClass(), "getData", (span) -> {
            // Fast path - no lock needed if already initialized
            if (!alwaysFetch() && isInitialized()) {
                recordSpanAttributes(span, true, false);
                return data;
            }

            lock.lock();
            try {
                // Double-check inside lock
                if (!alwaysFetch() && isInitialized()) {
                    recordSpanAttributes(span, true, false);
                    return data;
                }

                recordSpanAttributes(span, false, false);

                // Initialize data
                Optional<T> localData = tryInitDataMultipleTimes(realm, knownUser, tracingProvider);

                // Try delegate if needed (release lock first to avoid nested locking)
                if (localData.isEmpty() && getDelegate().isPresent()) {
                    lock.unlock();
                    try {
                        recordSpanAttributes(span, false, true);
                        localData = getDelegate().get().getData(realm, knownUser);
                    } finally {
                        lock.lock();
                    }

                    // Check if another thread initialized while we were calling delegate
                    if (this.data != null && this.data.isPresent()) {
                        return this.data;
                    }
                }

                // Store and return the result
                this.data = localData;
                return this.data;
            } finally {
                lock.unlock();
            }
        });
    }

    private void recordSpanAttributes(io.opentelemetry.api.trace.Span span, boolean alreadyInitialized, boolean usingDelegate) {
        if (span.isRecording()) {
            span.setAttribute("keycloak.user.context.always-fetch", alwaysFetch());
            span.setAttribute("keycloak.user.context.already-initialized", alreadyInitialized);
            if (usingDelegate) {
                span.setAttribute("keycloak.user.context.using-delegate", true);
            }
        }
    }

    @Override
    public Optional<UserContext<T>> getDelegate() {
        return Optional.ofNullable(delegate);
    }

    @Override
    public void setDelegate(UserContext<?> delegate) {
        this.delegate = (UserContext<T>) delegate;
    }

    protected Optional<T> tryInitDataMultipleTimes(RealmModel realm, UserModel knownUser, TracingProvider tracing) {
        for (int i = 0; i < COUNT_OF_INIT_RETRIES; i++) {
            Optional<T> data = tracing.trace(this.getClass(), "initData", (span) -> {
                return initData(realm, knownUser);
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
