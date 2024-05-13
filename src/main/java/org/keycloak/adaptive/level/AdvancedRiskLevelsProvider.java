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

import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;

import java.util.List;

public class AdvancedRiskLevelsProvider implements RiskLevelsProvider {
    static final RiskLevel LOW = new SimpleRiskLevel("LOW", 0.0, 0.2);
    static final RiskLevel MILD = new SimpleRiskLevel("MILD", 0.2, 0.4);
    static final RiskLevel MEDIUM = new SimpleRiskLevel("MEDIUM", 0.4, 0.6);
    static final RiskLevel MODERATE = new SimpleRiskLevel("MODERATE", 0.6, 0.8);
    static final RiskLevel HIGH = new SimpleRiskLevel("HIGH", 0.8, 1.0);

    @Override
    public List<RiskLevel> getRiskLevels() {
        return List.of(LOW, MILD, MEDIUM, MODERATE, HIGH);
    }
}
