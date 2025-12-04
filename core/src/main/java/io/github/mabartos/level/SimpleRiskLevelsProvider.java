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

import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.RiskLevelsProvider;

import java.util.List;

/**
 * Simple risk levels provider
 */
public class SimpleRiskLevelsProvider implements RiskLevelsProvider {
    static final RiskLevel LOW = new SimpleRiskLevel("LOW", 0.0, 0.33);
    static final RiskLevel MEDIUM = new SimpleRiskLevel("MEDIUM", 0.33, 0.66);
    static final RiskLevel HIGH = new SimpleRiskLevel("HIGH", 0.66, 1.0);

    @Override
    public List<RiskLevel> getRiskLevels() {
        return List.of(LOW, MEDIUM, HIGH);
    }
}
