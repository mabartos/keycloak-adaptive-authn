package io.github.mabartos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import java.io.IOException;
import java.io.InputStream;

public class AdaptiveRealmConfig implements RealmConfig {
    public static final String REALM_JSON_NAME = "test-adaptive-realm.json";

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(REALM_JSON_NAME)) {
            if (is == null) {
                throw new RuntimeException(REALM_JSON_NAME + " not found in classpath");
            }

            ObjectMapper mapper = new ObjectMapper();
            RealmRepresentation realmRep = mapper.readValue(is, RealmRepresentation.class);

            return RealmConfigBuilder.update(realmRep);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + REALM_JSON_NAME, e);
        }
    }
}
