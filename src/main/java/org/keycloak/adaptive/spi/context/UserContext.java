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

import org.keycloak.provider.Provider;

public abstract class UserContext<T> implements Provider {
    protected T data;
    protected boolean isInitialized;

    public boolean requiresUser() {
        return false;
    }

    public int getPriority() {
        return 0;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public abstract void initData();

    public T getData() {
        if (!isInitialized()) {
            initData();
        }
        return data;
    }

    @Override
    public void close() {
    }
}
