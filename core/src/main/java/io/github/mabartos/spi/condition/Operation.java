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

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Generic operation, that can be used during evaluating user context in conditions included in authentication flows
 *
 * @param <T> evaluated user context
 */
public class Operation<T extends UserContext<?>> {
    public static final String DEFAULT_MULTI_VALUES_DELIMITER = Constants.CFG_DELIMITER;

    private final String symbol;
    private final String text;
    private final BiPredicate<T, List<String>> condition;
    private final boolean isMultiValued;
    private final String multiValuedDelimiter;

    public Operation(String symbol, String text, BiPredicate<T, List<String>> condition, boolean isMultiValued, String multiValuedDelimiter) {
        this.symbol = symbol;
        this.text = text;
        this.condition = condition;
        this.isMultiValued = isMultiValued;
        this.multiValuedDelimiter = multiValuedDelimiter != null ? multiValuedDelimiter : DEFAULT_MULTI_VALUES_DELIMITER;
    }

    public Operation(String symbol, String text, BiPredicate<T, List<String>> condition, boolean isMultiValued) {
        this(symbol, text, condition, isMultiValued, null);
    }

    public Operation(String symbol, String text, BiPredicate<T, List<String>> condition) {
        this(symbol, text, condition, false);
    }

    public Operation(DefaultOperation.OperationKey ruleKey, BiPredicate<T, List<String>> condition) {
        this(ruleKey.symbol(), ruleKey.text(), condition, ruleKey.isMultiValued());
    }

    public String getSymbol() {
        return symbol;
    }

    public String getText() {
        return text;
    }

    public boolean isMultiValued() {
        return isMultiValued;
    }

    public String getMultiValuedDelimiter() {
        return multiValuedDelimiter;
    }

    public boolean match(T object, String value) {
        if (isMultiValued()) {
            return condition.test(object, value != null ?
                    List.of(value.split(getMultiValuedDelimiter())) :
                    List.of(value));
        } else {
            return condition.test(object, List.of(value));
        }
    }
}
