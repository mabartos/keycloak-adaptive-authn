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

/**
 * Default common operations made on user contexts
 */
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
