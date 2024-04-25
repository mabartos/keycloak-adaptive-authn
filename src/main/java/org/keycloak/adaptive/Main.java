package org.keycloak.adaptive;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.keycloak.adaptive.engine.AiTest;

@ApplicationScoped
public class Main {

   /* @Inject
    public AiTest test;*/

   /* @Startup
    public void asd() {
        var model = OpenAiChatModel.withApiKey("asdf");
        //AiTest test = AiServices.create(AiTest.class, model);
        System.err.println("ECHO");
        System.err.println(test.test("hello"));
    }*/

}
