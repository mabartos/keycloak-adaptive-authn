package io.github.mabartos.context.ssf;

import org.jboss.logging.Logger;
import org.keycloak.ssf.event.InitiatingEntity;

import java.util.Arrays;
import java.util.Optional;

public record SsfSignalData(SsfSignalType type, long timestamp, String source, InitiatingEntity initiatingEntity) {

    private static final Logger logger = Logger.getLogger(SsfSignalData.class);
    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_REGEX = "\\|";

    public static SsfSignalData of(SsfSignalType type) {
        return new SsfSignalData(type, System.currentTimeMillis(), null, null);
    }

    public static SsfSignalData of(SsfSignalType type, String source, InitiatingEntity initiatingEntity) {
        return new SsfSignalData(type, System.currentTimeMillis(), source, initiatingEntity);
    }

    public String formatToAttribute() {
        return type.name()
                + SEPARATOR + timestamp
                + SEPARATOR + Optional.ofNullable(source).orElse("")
                + SEPARATOR + (initiatingEntity != null ? initiatingEntity.getCode() : "");
    }

    public static SsfSignalData parseFromAttribute(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            String[] parts = value.split(SEPARATOR_REGEX, 4);
            if (parts.length < 2) {
                return null;
            }

            SsfSignalType type = SsfSignalType.valueOf(parts[0]);
            long timestamp = Long.parseLong(parts[1]);
            String source = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            InitiatingEntity initiator = parts.length > 3 && !parts[3].isEmpty()
                    ? parseInitiatingEntity(parts[3])
                    : null;

            return new SsfSignalData(type, timestamp, source, initiator);
        } catch (IllegalArgumentException e) {
            logger.debugf("Failed to parse SSF signal attribute: %s", value);
            return null;
        }
    }

    private static InitiatingEntity parseInitiatingEntity(String code) {
        return Arrays.stream(InitiatingEntity.values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
