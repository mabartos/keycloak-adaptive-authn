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

import java.util.List;

/**
 * Simple 3-level risk levels: LOW, MEDIUM, HIGH.
 * Validates that levels cover the entire 0-1 spectrum on construction.
 */
public class SimpleRiskLevels implements RiskLevels {
    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String HIGH = "HIGH";

    private final List<RiskLevel> levels;

    public SimpleRiskLevels(RiskLevel low, RiskLevel medium, RiskLevel high) {
        this.levels = List.of(low, medium, high);
        validate();
    }

    @Override
    public List<RiskLevel> getLevels() {
        return levels;
    }

    @Override
    public List<String> getLevelNames() {
        return getSimpleLevelNames();
    }

    /**
     * Get all simple risk level names in order (static access)
     */
    public static List<String> getSimpleLevelNames() {
        return List.of(LOW, MEDIUM, HIGH);
    }

    /**
     * Get description of simple risk levels
     */
    public static String getDescription() {
        return "simple (3-level)";
    }

    @Override
    public void validate() {
        RiskLevelValidator.validate(levels, "SimpleRiskLevels");
    }
}
