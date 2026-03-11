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
package io.github.mabartos.spi.level;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for risk levels to ensure they cover the entire 0-1 spectrum with no gaps or overlaps.
 */
public final class RiskLevelValidator {

    private RiskLevelValidator() {
        // Utility class
    }

    /**
     * Validates that risk levels cover the entire 0-1 spectrum with no gaps or overlaps.
     *
     * @param levels the risk levels to validate
     * @param context context for error messages (e.g., "SimpleRiskLevels", "LogOddsRiskAlgorithm")
     * @throws IllegalStateException if validation fails
     */
    public static void validate(List<RiskLevel> levels, String context) {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalStateException(String.format("[%s] Risk levels cannot be null or empty", context));
        }

        List<String> errors = new ArrayList<>();

        // Check first level starts at 0.0
        RiskLevel first = levels.get(0);
        if (first.lowestRiskValue() != 0.0) {
            errors.add(String.format("First risk level '%s' must start at 0.0 but starts at %.2f",
                    first.name(), first.lowestRiskValue()));
        }

        // Check last level ends at 1.0
        RiskLevel last = levels.get(levels.size() - 1);
        if (last.highestRiskValue() != 1.0) {
            errors.add(String.format("Last risk level '%s' must end at 1.0 but ends at %.2f",
                    last.name(), last.highestRiskValue()));
        }

        // Check for gaps and overlaps between consecutive levels
        for (int i = 0; i < levels.size() - 1; i++) {
            RiskLevel current = levels.get(i);
            RiskLevel next = levels.get(i + 1);

            double currentHigh = current.highestRiskValue();
            double nextLow = next.lowestRiskValue();

            // Check for gaps
            if (currentHigh < nextLow) {
                errors.add(String.format("Gap detected: '%s' ends at %.2f but '%s' starts at %.2f (gap: %.2f-%.2f)",
                        current.name(), currentHigh, next.name(), nextLow, currentHigh, nextLow));
            }

            // Check for overlaps (allowing exact match where one ends and next begins)
            if (currentHigh > nextLow) {
                errors.add(String.format("Overlap detected: '%s' ends at %.2f but '%s' starts at %.2f",
                        current.name(), currentHigh, next.name(), nextLow));
            }

            // Levels should be contiguous (current.high == next.low)
            if (currentHigh != nextLow) {
                errors.add(String.format("Levels not contiguous: '%s' ends at %.2f but '%s' starts at %.2f",
                        current.name(), currentHigh, next.name(), nextLow));
            }

            // Check ascending order
            if (current.lowestRiskValue() >= next.lowestRiskValue()) {
                errors.add(String.format("Levels not in ascending order: '%s' (%.2f-%.2f) should come before '%s' (%.2f-%.2f)",
                        current.name(), current.lowestRiskValue(), current.highestRiskValue(),
                        next.name(), next.lowestRiskValue(), next.highestRiskValue()));
            }
        }

        if (!errors.isEmpty()) {
            String errorMessage = String.format("Risk level validation failed for '%s':\n- %s",
                    context, String.join("\n- ", errors));
            throw new IllegalStateException(errorMessage);
        }
    }
}
