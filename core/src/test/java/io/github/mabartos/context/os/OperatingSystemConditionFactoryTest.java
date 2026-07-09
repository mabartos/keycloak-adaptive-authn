package io.github.mabartos.context.os;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.account.DeviceRepresentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Device OS values mirror Keycloak {@code DeviceRepresentationProviderImpl} + ua-parser output.
 */
class OperatingSystemConditionFactoryTest {

    // DeviceRepresentationProviderImpl: Windows NT 10.0 -> osVersion "10"
    private static final DeviceRepresentation WINDOWS_10 = keycloakDevice("Windows", "10");
    private static final DeviceRepresentation WINDOWS_7 = keycloakDevice("Windows", "7");
    private static final DeviceRepresentation WINDOWS_81 = keycloakDevice("Windows", "8.1");
    private static final DeviceRepresentation LINUX = keycloakDevice("Linux", null);
    private static final DeviceRepresentation MAC = keycloakDevice("Mac OS X", "10.15.7");

    @Test
    void recentWindowsVersionMatchesKeycloakNt10Output() {
        assertTrue(OperatingSystemConditionFactory.isRecentWindowsVersion("10"));
        assertTrue(OperatingSystemConditionFactory.isRecentWindowsVersion("10.0"));
        assertFalse(OperatingSystemConditionFactory.isRecentWindowsVersion("7"));
        assertFalse(OperatingSystemConditionFactory.isRecentWindowsVersion("8.1"));
        assertFalse(OperatingSystemConditionFactory.isRecentWindowsVersion(null));
    }

    @Test
    void trustedOsFromKeycloakDeviceRepresentation() {
        assertTrue(OperatingSystemConditionFactory.isTrustedOs(WINDOWS_10));
        assertTrue(OperatingSystemConditionFactory.isTrustedOs(LINUX));
        assertTrue(OperatingSystemConditionFactory.isTrustedOs(MAC));
    }

    @Test
    void legacyWindowsFromKeycloakDeviceRepresentation() {
        assertTrue(OperatingSystemConditionFactory.isLegacyWindows(WINDOWS_7));
        assertTrue(OperatingSystemConditionFactory.isLegacyWindows(WINDOWS_81));
        assertFalse(OperatingSystemConditionFactory.isLegacyWindows(WINDOWS_10));
    }

    @Test
    void macOsMatchesDefaultPrefix() {
        assertTrue(OperatingSystemConditionFactory.isOs(MAC, DefaultOperatingSystems.MAC));
    }

    @Test
    void unknownOsIsNotTrustedOrLegacyWindows() {
        DeviceRepresentation android = keycloakDevice("Android", "14");

        assertFalse(OperatingSystemConditionFactory.isTrustedOs(android));
        assertFalse(OperatingSystemConditionFactory.isLegacyWindows(android));
    }

    private static DeviceRepresentation keycloakDevice(String os, String osVersion) {
        DeviceRepresentation device = new DeviceRepresentation();
        device.setOs(os);
        device.setOsVersion(osVersion);
        return device;
    }
}
