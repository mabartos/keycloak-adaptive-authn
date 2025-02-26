/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adaptive.level;

import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Common risk values
 */
public class Risk {
    public static double NONE = 0.0;
    public static double SMALL = 0.3;
    public static double MEDIUM = 0.5;
    public static double INTERMEDIATE = 0.7;
    public static double VERY_HIGH = 0.85;
    public static double HIGHEST = 1.0;

    private static final Risk INVALID = of(-1);
    private static final Risk NO_RISK = of(0);

    private final boolean valid;
    private final double score;
    private final String reason;

    private Risk(double score, String reason) {
        this.valid = isValid(score);
        this.score = score;
        this.reason = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public Optional<Double> getScore() {
        return isValid() ? Optional.of(score) : Optional.empty();
    }

    public Optional<String> getReason() {
        return StringUtil.isNotBlank(reason) ? Optional.of(reason) : Optional.empty();
    }

    public static Risk of(double risk) {
        return of(risk, "");
    }

    public static Risk of(double risk, String reason) {
        return new Risk(risk, reason);
    }

    public static Risk invalid() {
        return INVALID;
    }

    public static Risk none() {
        return NO_RISK;
    }

    public static boolean isValid(double score) {
        return score >= 0.0d && score <= 1.0d;
    }
}
