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
package org.keycloak.adaptive.spi.condition;

import org.keycloak.adaptive.spi.context.UserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Builder for the {@link Operation} model
 *
 * @param <T> evaluated user context
 */
public class OperationsBuilder<T extends UserContext<?>> {
    private final List<Operation<T>> operations;

    private OperationsBuilder() {
        this.operations = new ArrayList<>();
    }

    public static <U extends UserContext<?>> OperationsBuilder<U> builder(Class<U> ignore) {
        return new OperationsBuilder<>();
    }

    public OperationBuilder<T> operation() {
        return new OperationBuilder<>();
    }

    public List<Operation<T>> build() {
        return operations;
    }

    public class OperationBuilder<U> {
        private String symbol = "";
        private String text = "";
        private BiPredicate<T, String> condition = (k, v) -> false;

        private OperationBuilder() {
        }

        public OperationBuilder<U> symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public OperationBuilder<U> text(String text) {
            this.text = text;
            return this;
        }

        public OperationBuilder<U> operationKey(DefaultOperation.OperationKey ruleKey) {
            symbol(ruleKey.symbol());
            text(ruleKey.text());
            return this;
        }

        public OperationBuilder<U> condition(BiPredicate<T, String> condition) {
            this.condition = condition;
            return this;
        }

        public OperationsBuilder<T> add() {
            OperationsBuilder.this.operations.add(new Operation<T>(symbol, text, condition));
            return OperationsBuilder.this;
        }
    }
}