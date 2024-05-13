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

import org.keycloak.adaptive.policy.DefaultOperation;
import org.keycloak.adaptive.spi.context.UserContext;

import java.util.function.BiPredicate;

public class Operation<T extends UserContext<?>> {
    private final String symbol;
    private final String text;
    private final BiPredicate<T, String> condition;

    public Operation(String symbol, String text, BiPredicate<T, String> condition) {
        this.symbol = symbol;
        this.text = text;
        this.condition = condition;
    }

    public Operation(DefaultOperation.OperationKey ruleKey, BiPredicate<T, String> condition) {
        this(ruleKey.symbol(), ruleKey.text(), condition);
    }

    public String getSymbol() {
        return symbol;
    }

    public String getText() {
        return text;
    }

    public boolean match(T object, String value) {
        return condition.test(object, value);
    }
}
