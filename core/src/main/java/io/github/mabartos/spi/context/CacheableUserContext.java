package io.github.mabartos.spi.context;

import org.keycloak.models.RealmModel;

public interface CacheableUserContext<T> {

    void updateCache(RealmModel realm, T data);
}
