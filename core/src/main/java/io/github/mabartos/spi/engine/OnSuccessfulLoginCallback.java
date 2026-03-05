package io.github.mabartos.spi.engine;

import jakarta.annotation.Nonnull;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@FunctionalInterface
public interface OnSuccessfulLoginCallback {

    void onSuccessfulLogin(@Nonnull RealmModel realm, @Nonnull UserModel user);
}
