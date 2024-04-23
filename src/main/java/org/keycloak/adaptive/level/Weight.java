package org.keycloak.adaptive.level;

public interface Weight {
    double NEGLIGIBLE = 0.1;
    double LOW = 0.3;
    double NORMAL = 0.5;
    double IMPORTANT = 0.8;
    double HIGHEST = 1;

    double DEFAULT = NORMAL;
}
