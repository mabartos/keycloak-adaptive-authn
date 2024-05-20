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

/**
 * Fundamental implementation of the risk level properties
 */
public class SimpleRiskLevel implements RiskLevel {
    private final String name;
    private double lowestRiskValue;
    private double highestRiskValue;

    public SimpleRiskLevel(String name, double lowestRiskValue, double highestRiskValue) {
        this.name = name;
        this.lowestRiskValue = lowestRiskValue;
        this.highestRiskValue = highestRiskValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getLowestRiskValue() {
        return lowestRiskValue;
    }

    public void setLowestRiskValue(double value) {
        this.lowestRiskValue = value;
    }

    @Override
    public double getHighestRiskValue() {
        return highestRiskValue;
    }

    public void setHighestRiskValue(double value) {
        this.highestRiskValue = value;
    }

    @Override
    public boolean matchesRisk(double riskValue) {
        if (getLowestRiskValue() == 0.0f && riskValue == getLowestRiskValue()) return true;
        return riskValue > getLowestRiskValue() && riskValue <= getHighestRiskValue();
    }
}
