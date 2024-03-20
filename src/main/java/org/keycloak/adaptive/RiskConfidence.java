package org.keycloak.adaptive;

public interface RiskConfidence {
    double NONE = 0.0;
    double SMALL = 0.3;
    double MEDIUM = 0.5;
    double PRETTY_CONFIDENT = 0.7;
    double VERY_CONFIDENT = 0.9;
    double HIGHEST = 1.0;
}
