package org.keycloak.adaptive.engine;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import org.keycloak.representations.account.DeviceRepresentation;

//@RegisterAiService
/*@SystemMessage("""
        You evaluate a risk that user who is trying to authenticate is fraud.
        You return the double value in range (0,1>, as f.e. 0.8 means 80% change of the authentication attempt is very critical.
                
        - Values close to 0 means the risk the user is fraud is low.
        - Values close to 1 means the risk the user is fraud is high.
                
        You need to analyze data I provide to you and return risk values.
        """)*/
public interface AiRiskEngine {

    @UserMessage("""
            Evaluate risk based on information about device, which is trying to access the application.
                - Operating system: {device.getOs()}
            """)
    Double evaluateRiskDevice(DeviceRepresentation device);
}
