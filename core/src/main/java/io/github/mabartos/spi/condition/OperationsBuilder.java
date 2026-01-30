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
package io.github.mabartos.spi.condition;

import io.github.mabartos.spi.context.UserContext;
import org.keycloak.models.Constants;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Builder for the {@link Operation} model
 *
 * @param <T> evaluated user context
 */
public class OperationsBuilder<T extends UserContext<?>> {
    private final List<Operation<T>> operations;
    private final String inputType;

    private OperationsBuilder(String inputType) {
        this.inputType = inputType;
        this.operations = new ArrayList<>();
    }

    public static <U extends UserContext<?>> OperationsBuilder<U> builder(Class<U> ignore, String inputType) {
        return new OperationsBuilder<>(inputType);
    }

    public OperationBuilder<T> operation() {
        return new OperationBuilder<>();
    }

    public List<Operation<T>> build() {
        return operations;
    }

    private static final Map<String, String> INPUT_TYPE_MULTIVALUED_DELIMITERS = Map.of(
            ProviderConfigProperty.STRING_TYPE, ","
    );

    public class OperationBuilder<U> {
        private String symbol;
        private String text = "";
        private boolean isMultiValued;
        private String multiValuedDelimiter;
        private BiPredicate<T, List<String>> condition = (k, v) -> false;

        private OperationBuilder() {
            this.multiValuedDelimiter = Optional.ofNullable(inputType)
                    .map(INPUT_TYPE_MULTIVALUED_DELIMITERS::get)
                    .orElse(null);
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
            this.condition = (obj, list) -> condition.test(obj, list.getFirst());
            return this;
        }

        public OperationBuilder<U> multiValuedCondition(BiPredicate<T, List<String>> condition) {
            return multiValuedCondition(condition, null);
        }

        public OperationBuilder<U> multiValuedCondition(BiPredicate<T, List<String>> condition, String valuesDelimiter) {
            this.isMultiValued = true;
            this.condition = condition;
            this.multiValuedDelimiter = valuesDelimiter;
            return this;
        }

        public OperationsBuilder<T> add() {
            if (StringUtil.isBlank(symbol)) {
                throw new IllegalArgumentException("Symbol for operation cannot be empty or null");
            }
            operations.add(new Operation<T>(symbol, text, condition, isMultiValued, multiValuedDelimiter));
            return OperationsBuilder.this;
        }
    }
}