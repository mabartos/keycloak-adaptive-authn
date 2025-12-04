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

import io.github.mabartos.spi.level.RiskLevelsFactory;
import io.github.mabartos.spi.level.RiskLevelsProvider;

public class SimpleRiskLevelsFactory implements RiskLevelsFactory {
    public static final String PROVIDER_ID = "simple-risk-levels";
    private static final RiskLevelsProvider SINGLETON = new SimpleRiskLevelsProvider();

    @Override
    public RiskLevelsProvider getSingleton() {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Simple";
    }

    @Override
    public String getHelpText() {
        return "Risk levels - 3 levels (Low, Medium, High)";
    }
}
