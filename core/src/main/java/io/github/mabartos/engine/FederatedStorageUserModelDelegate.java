package io.github.mabartos.engine;

import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import static org.keycloak.common.util.CollectionUtil.isNotEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // Marker for attributes pending removal in the pendingWrites map.
    // ConcurrentHashMap does not allow null values, so we use this empty list
    // to distinguish "attribute was removed" from "no pending change for this attribute".
    private static final List<String> REMOVED_ATTRIBUTE = Collections.emptyList();

    private final KeycloakSession session;
    private final RealmModel realm;
    private final boolean localStorage;
    private final Map<String, List<String>> pendingWrites = new ConcurrentHashMap<>();
    private volatile boolean transactionEnlisted = false;

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
            pendingWrites.put(name, List.of(value));
            enlistTransaction();
            return;
        }
        super.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (!localStorage) {
            pendingWrites.put(name, values);
            enlistTransaction();
            return;
        }
        super.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        if (!localStorage) {
            pendingWrites.put(name, REMOVED_ATTRIBUTE);
            enlistTransaction();
            return;
        }
        super.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (!localStorage) {
            List<String> pending = pendingWrites.get(name);
            if (pending != null) {
                return (pending != REMOVED_ATTRIBUTE && !pending.isEmpty()) ? pending.getFirst() : null;
            }
            List<String> values = federatedStorage().getAttributes(realm, getId()).get(name);
            return isNotEmpty(values) ? values.getFirst() : null;
        }
        return super.getFirstAttribute(name);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (!localStorage) {
            List<String> pending = pendingWrites.get(name);
            if (pending != null) {
                return (pending != REMOVED_ATTRIBUTE) ? pending.stream() : Stream.empty();
            }
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
            for (var entry : pendingWrites.entrySet()) {
                if (entry.getValue() == REMOVED_ATTRIBUTE) {
                    attributes.remove(entry.getKey());
                } else {
                    attributes.put(entry.getKey(), entry.getValue());
                }
            }
            return attributes;
        }
        return super.getAttributes();
    }

    private void enlistTransaction() {
        if (!transactionEnlisted) {
            transactionEnlisted = true;
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    flushPendingWrites();
                }

                @Override
                protected void rollbackImpl() {
                    pendingWrites.clear();
                }
            });
        }
    }

    private void flushPendingWrites() {
        var realmId = realm.getId();
        var userId = getId();
        Map<String, List<String>> writesToFlush = new HashMap<>(pendingWrites);
        pendingWrites.clear();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            var freshRealm = s.realms().getRealm(realmId);
            var storage = UserStorageUtil.userFederatedStorage(s);
            for (var entry : writesToFlush.entrySet()) {
                if (entry.getValue() == REMOVED_ATTRIBUTE) {
                    storage.removeAttribute(freshRealm, userId, entry.getKey());
                } else {
                    storage.setAttribute(freshRealm, userId, entry.getKey(), entry.getValue());
                }
            }
        });
    }

    private UserFederatedStorageProvider federatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }
}
