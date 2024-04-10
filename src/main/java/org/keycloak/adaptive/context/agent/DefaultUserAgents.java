package org.keycloak.adaptive.context.agent;

import java.util.Set;

public class DefaultUserAgents {
    public static final String FIREFOX = "Firefox";
    public static final String CHROME = "Chrome";
    public static final String SAFARI = "Safari";

    public static final Set<String> KNOWN_AGENTS = Set.of(FIREFOX, CHROME, SAFARI);
}
