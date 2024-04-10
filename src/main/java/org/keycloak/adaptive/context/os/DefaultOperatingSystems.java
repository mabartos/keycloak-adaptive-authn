package org.keycloak.adaptive.context.os;

import java.util.Set;

// TODO check values
public interface DefaultOperatingSystems {
    String LINUX = "Linux";
    String WINDOWS = "Windows";
    String MAC = "Mac";

    Set<String> DEFAULT_OPERATING_SYSTEMS = Set.of(LINUX, WINDOWS, MAC);
}
