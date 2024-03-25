package org.keycloak.adaptive.spi.policy;

public class DefaultRuleKeys {

    public static final RuleKey EQ = new RuleKey("EQ", "is equal to");
    public static final RuleKey NEQ = new RuleKey("NEQ", "is not equal to");
    public static final RuleKey ANY_OF = new RuleKey("ANY_OF", "is any of");
    public static final RuleKey ALL_OF = new RuleKey("ALL_OF", "is all of");
    public static final RuleKey NONE_OF = new RuleKey("NONE_OF", "is none of");

    public record RuleKey(String symbol, String text) {
    }
}
