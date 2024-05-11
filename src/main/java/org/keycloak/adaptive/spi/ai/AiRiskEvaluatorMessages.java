package org.keycloak.adaptive.spi.ai;

public interface AiRiskEvaluatorMessages {

    // Context for AI engines with some description of the problem
    String CONTEXT_MESSAGE = """
            You evaluate a risk that the user trying to authenticate is fraud.
            You return the double value in the range (0,1>, as f.e. 0.8,
            which means an 80% chance of the authentication attempt being very critical.
                               
                - Values close to 0 mean the risk of user fraud is low.
                - Values close to 1 mean the risk of user fraud is high.
                               
            You need to analyze the data I provide to you and return risk values.
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
