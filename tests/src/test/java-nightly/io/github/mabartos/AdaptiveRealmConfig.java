package io.github.mabartos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

import java.io.IOException;
import java.io.InputStream;

/**
 * Realm config for Keycloak nightly ({@code 999.0.0-SNAPSHOT}) using {@link RealmBuilder}
 * after <a href="https://github.com/keycloak/keycloak/pull/48315">keycloak#48315</a>.
 */
public class AdaptiveRealmConfig implements RealmConfig {
    public static final String REALM_JSON_NAME = "test-adaptive-realm.json";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(REALM_JSON_NAME)) {
            if (is == null) {
                throw new RuntimeException(REALM_JSON_NAME + " not found in classpath");
            }

            ObjectMapper mapper = new ObjectMapper();
            RealmRepresentation realmRep = mapper.readValue(is, RealmRepresentation.class);

            return RealmBuilder.update(realmRep);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + REALM_JSON_NAME, e);
        }
    }
}
