package io.github.mabartos.context.ssf;

import io.github.mabartos.spi.level.Risk;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;

import static io.github.mabartos.spi.level.Risk.Score.EXTREME;
import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.VERY_HIGH;

public enum SsfSignalType {

    CREDENTIAL_CHANGE(MEDIUM, CaepCredentialChange.TYPE),
    CREDENTIAL_REVOKED(VERY_HIGH, CaepCredentialChange.TYPE),
    SESSION_REVOKED(HIGH, CaepSessionRevoked.TYPE),
    ACCOUNT_DISABLED(VERY_HIGH),
    ACCOUNT_RECOVERY_ACTIVATED(EXTREME),
    ASSURANCE_LEVEL_DECREASED(MEDIUM),
    DEVICE_COMPLIANCE_CHANGED(MEDIUM);

    private final Risk.Score baseRisk;
    private final String caepEventType;

    SsfSignalType(Risk.Score baseRisk) {
        this(baseRisk, null);
    }

    SsfSignalType(Risk.Score baseRisk, String caepEventType) {
        this.baseRisk = baseRisk;
        this.caepEventType = caepEventType;
    }

    public Risk.Score getBaseRisk() {
        return baseRisk;
    }

    public String getCaepEventType() {
        return caepEventType;
    }
}
