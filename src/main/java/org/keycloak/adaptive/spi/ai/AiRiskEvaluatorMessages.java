package org.keycloak.adaptive.spi.ai;

public interface AiRiskEvaluatorMessages {

    // Context for AI engines with some description of the problem
    String CONTEXT_MESSAGE = """
            You evaluate a risk that the user trying to authenticate is fraud.
            You return the double value in the range (0,1>, as f.e. 0.8, which means an 80% chance of the authentication attempt being very critical.
                               
                - Values close to 0 mean the risk of user fraud is low.
                - Values close to 1 mean the risk of user fraud is high.
                               
            You need to analyze the data I provide to you and return risk values.
            The message MUST contain only the double value, no text around - gave me only the number without further explanations.
            """;
}
