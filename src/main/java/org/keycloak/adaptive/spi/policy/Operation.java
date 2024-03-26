package org.keycloak.adaptive.spi.policy;

import java.util.function.BiPredicate;

public class Operation<T> {
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
