package org.keycloak.adaptive.spi.policy;

import java.util.Map;
import java.util.function.BiPredicate;

public class UserContextRule<T> implements Map.Entry<String, UserContextRule<T>> {
    private final String symbol;
    private final String text;
    private final BiPredicate<T, String> condition;

    public UserContextRule(String symbol, String text, BiPredicate<T, String> condition) {
        this.symbol = symbol;
        this.text = text;
        this.condition = condition;
    }

    public UserContextRule(DefaultRuleKeys.RuleKey ruleKey, BiPredicate<T, String> condition) {
        this(ruleKey.symbol(), ruleKey.text(), condition);
    }

    public String getText() {
        return text;
    }

    public boolean match(T object, String value) {
        return condition.test(object, value);
    }

    @Override
    public String getKey() {
        return symbol;
    }

    @Override
    public UserContextRule<T> getValue() {
        return this;
    }

    @Override
    public UserContextRule<T> setValue(UserContextRule<T> tUserContextRule) {
        return null;
    }
}
