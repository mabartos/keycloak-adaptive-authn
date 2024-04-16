package org.keycloak.adaptive.level;

public interface Risk {
    double NONE = 0.01;
    double SMALL = 0.3;
    double MEDIUM = 0.5;
    double INTERMEDIATE = 0.7;
    double HIGH = 1.0;
}
