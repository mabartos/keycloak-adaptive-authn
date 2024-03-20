package org.keycloak.adaptive.context.agent;

import java.util.Set;

public class DefaultUserAgents {
    public static final UserAgent MOZILLA = () -> "Mozilla";
    public static final UserAgent CHROME = () -> "Chrome";
    public static final UserAgent SAFARI = () -> "Safari";

    public static final Set<UserAgent> KNOWN_AGENTS = Set.of(MOZILLA, CHROME, SAFARI);
}
