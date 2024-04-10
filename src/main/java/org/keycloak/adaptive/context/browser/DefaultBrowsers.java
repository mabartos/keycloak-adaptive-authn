package org.keycloak.adaptive.context.browser;

import java.util.Set;

public interface DefaultBrowsers {
    String FIREFOX = "Firefox";
    String CHROME = "Chrome";
    String SAFARI = "Safari";

    Set<String> DEFAULT_BROWSERS = Set.of(FIREFOX, CHROME, SAFARI);
}
