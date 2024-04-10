package org.keycloak.adaptive.spi.policy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

public class OperationsBuilder<T> {
    private final Set<Operation<T>> operations;

    private OperationsBuilder() {
        this.operations = new HashSet<>();
    }

    public static <U> OperationsBuilder<U> builder(Class<U> ignore) {
        return new OperationsBuilder<>();
    }

    public OperationBuilder<T> operation() {
        return new OperationBuilder<>();
    }

    public Set<Operation<T>> build() {
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