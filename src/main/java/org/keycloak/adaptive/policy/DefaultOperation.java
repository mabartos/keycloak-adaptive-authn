package org.keycloak.adaptive.policy;

public class DefaultOperation {

    public static final OperationKey IS = new OperationKey("IS", "is");
    public static final OperationKey EQ = new OperationKey("EQ", "is equal to");
    public static final OperationKey NEQ = new OperationKey("NEQ", "is not equal to");
    public static final OperationKey ANY_OF = new OperationKey("ANY_OF", "is any of");
    public static final OperationKey ALL_OF = new OperationKey("ALL_OF", "is all of");
    public static final OperationKey NONE_OF = new OperationKey("NONE_OF", "is none of");
    public static final OperationKey IN_RANGE = new OperationKey("IN_RANGE", "is in range");
    public static final OperationKey NOT_IN_RANGE = new OperationKey("NOT_IN_RANGE", "is not in range");

    public record OperationKey(String symbol, String text) {
    }
}
