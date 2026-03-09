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
package io.github.mabartos.level;

import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Common risk values
 */
public class Risk {
    public enum Score {
        INVALID,
        NEGATIVE_HIGH,
        NEGATIVE_LOW,
        NONE,
        VERY_SMALL,
        SMALL,
        MEDIUM,
        HIGH,
        VERY_HIGH,
        EXTREME,
    }

    private final Score scoreCategory;
    private final String reason;

    private Risk(Score scoreCategory, String reason) {
        this.scoreCategory = scoreCategory;
        this.reason = reason;
    }

    public Score getScore() {
        return scoreCategory;
    }

    public Optional<String> getReason() {
        return StringUtil.isNotBlank(reason) ? Optional.of(reason) : Optional.empty();
    }

    public boolean isValid() {
        return scoreCategory != Score.INVALID;
    }

    public static Risk of(Score score) {
        return of(score, "");
    }

    public static Risk of(Score score, String reason) {
        return new Risk(score, reason);
    }

    public static Risk invalid(String reason) {
        return new Risk(Score.INVALID, reason);
    }

    public Risk max(Risk risk) {
        if (risk == null || risk.getScore() == Score.INVALID) {
            return this;
        }
        return getScore().ordinal() >= risk.getScore().ordinal() ? this : risk;
    }
}
