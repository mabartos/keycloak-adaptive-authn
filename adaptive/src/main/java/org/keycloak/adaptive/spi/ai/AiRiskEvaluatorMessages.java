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
package org.keycloak.adaptive.spi.ai;

public interface AiRiskEvaluatorMessages {

    // Context for AI engines with some description of the problem
    String CONTEXT_MESSAGE = """
            Evaluate a risk that the user trying to authenticate is fraud.
            Return the double value in the range (0,1>, as f.e. 0.8,
            which means an 80% chance of the authentication attempt being very critical.
                               
                - Values close to 0 mean the risk of user fraud is low.
                - Values close to 1 mean the risk of user fraud is high.
                               
            Analyze the provided data and return risk values.
            The message MUST be in JSON format, with two items - 'risk' and 'reason'.
            The 'risk' item MUST contain the evaluated risk double value described above.
            The 'reason' item MUST briefly describe the reason why it was evaluated like that.
                        
            f.e.
             {
               "risk": 0.7,
               "reason": "Many login failures, with a high probability of brute-force attack."
             }
            """;
}
