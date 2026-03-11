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
 * Wrapper for a collection of risk levels.
 * Encapsulates the levels, their names, and validation logic.
 */
public interface RiskLevels {

    /**
     * Get the list of risk levels in order
     *
     * @return list of risk levels
     */
    List<RiskLevel> getLevels();

    /**
     * Get the names of risk levels in order
     *
     * @return list of level names
     */
    List<String> getLevelNames();

    /**
     * Validates that risk levels cover the entire 0-1 spectrum with no gaps or overlaps.
     *
     * @throws IllegalStateException if validation fails
     */
    void validate();
}
