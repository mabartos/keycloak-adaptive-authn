package org.keycloak.adaptive.spi.condition;

import org.keycloak.adaptive.policy.DefaultOperation;
import org.keycloak.adaptive.spi.context.UserContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

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