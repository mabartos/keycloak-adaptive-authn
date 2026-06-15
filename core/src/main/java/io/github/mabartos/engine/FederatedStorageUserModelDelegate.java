package io.github.mabartos.engine;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import static org.keycloak.common.util.CollectionUtil.isNotEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Transparently routes attribute operations to Keycloak's federated storage
 * for non-local (non-imported) users, e.g. LDAP users with import disabled.
 * <p>
 * This avoids {@link org.keycloak.storage.ReadOnlyException} when the extension
 * needs to persist adaptive authentication data alongside read-only federated users.
 * Federated storage writes to Keycloak's own database, not to the external source.
 */
public class FederatedStorageUserModelDelegate extends UserModelDelegate {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final boolean localStorage;

    private FederatedStorageUserModelDelegate(UserModel delegate, KeycloakSession session, RealmModel realm) {
        super(delegate);
        this.session = session;
        this.realm = realm;
        this.localStorage = StorageId.isLocalStorage(delegate.getId());
    }

    /**
     * Wraps the user model to route attribute operations to federated storage
     * if the user is non-local (e.g. non-imported LDAP with READ_ONLY mode).
     * Returns the original user model unchanged for local/imported users.
     */
    public static UserModel wrapIfNeeded(UserModel user, KeycloakSession session, RealmModel realm) {
        if (user == null || StorageId.isLocalStorage(user.getId())) {
            return user;
        }
        return new FederatedStorageUserModelDelegate(user, session, realm);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (!localStorage) {
            federatedStorage().setSingleAttribute(realm, getId(), name, value);
            return;
        }
        super.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (!localStorage) {
            federatedStorage().setAttribute(realm, getId(), name, values);
            return;
        }
        super.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        if (!localStorage) {
            federatedStorage().removeAttribute(realm, getId(), name);
            return;
        }
        super.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (!localStorage) {
            List<String> values = federatedStorage().getAttributes(realm, getId()).get(name);
            return isNotEmpty(values) ? values.getFirst() : null;
        }
        return super.getFirstAttribute(name);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (!localStorage) {
            List<String> values = federatedStorage().getAttributes(realm, getId()).get(name);
            return isNotEmpty(values) ? values.stream() : Stream.empty();
        }
        return super.getAttributeStream(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (!localStorage) {
            Map<String, List<String>> attributes = new HashMap<>(super.getAttributes());
            attributes.putAll(federatedStorage().getAttributes(realm, getId()));
            return attributes;
        }
        return super.getAttributes();
    }

    private UserFederatedStorageProvider federatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }
}
