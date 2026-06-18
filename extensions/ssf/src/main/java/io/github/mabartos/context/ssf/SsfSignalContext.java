package io.github.mabartos.context.ssf;

import io.github.mabartos.spi.context.AbstractUserContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SsfSignalContext extends AbstractUserContext<List<SsfSignalData>> {

    private static final Logger logger = Logger.getLogger(SsfSignalContext.class);

    public static final String SSF_SIGNALS_ATTR = "adaptive-ssf-signals";
    static final int MAX_STORED_SIGNALS = 20;

    public SsfSignalContext(KeycloakSession session) {
        super(session);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean alwaysFetch() {
        return false;
    }

    @Override
    public Optional<List<SsfSignalData>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            logger.trace("User is null");
            return Optional.empty();
        }

        List<SsfSignalData> signals = knownUser.getAttributeStream(SSF_SIGNALS_ATTR)
                .map(SsfSignalData::parseFromAttribute)
                .filter(Objects::nonNull)
                .toList();

        return Optional.of(signals);
    }

    public static void recordSignal(UserModel user, SsfSignalData signal) {
        if (user == null || signal == null) {
            return;
        }

        LinkedList<String> existing = new LinkedList<>(
                user.getAttributeStream(SSF_SIGNALS_ATTR).toList()
        );

        existing.add(signal.formatToAttribute());

        while (existing.size() > MAX_STORED_SIGNALS) {
            existing.removeFirst();
        }

        user.setAttribute(SSF_SIGNALS_ATTR, new ArrayList<>(existing));
    }
}
