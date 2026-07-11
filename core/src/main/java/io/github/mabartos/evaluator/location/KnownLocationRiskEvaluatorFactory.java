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
package io.github.mabartos.evaluator.location;

import io.github.mabartos.context.location.KnownLocationSettings;
import io.github.mabartos.evaluator.EvaluatorSettingProperties;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class KnownLocationRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "known-location-risk-evaluator";
    public static final String NAME = "Known location";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Compares the current login location (GeoIP) to the user's known locations after identification. Requires location context, enable Init location if this evaluator is active.";
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return KnownLocationRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new KnownLocationRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalAdminConfigProperties() {
        var evaluatorClass = KnownLocationRiskEvaluator.class;
        return EvaluatorSettingProperties.of(
                EvaluatorSettingProperties.intProperty(
                        evaluatorClass, KnownLocationSettings.TTL_DAYS_SETTING_KEY,
                        "TTL (days)",
                        "Number of days before a known location stops providing a trust signal. "
                                + "Expired entries are removed on successful login. Set to 0 to disable expiration.",
                        KnownLocationSettings.DEFAULT_TTL_DAYS,
                        0),
                EvaluatorSettingProperties.intProperty(
                        evaluatorClass, KnownLocationSettings.MAX_STORED_LOCATIONS_SETTING_KEY,
                        "Max stored locations",
                        "Maximum number of known locations kept per user (minimum 1).",
                        KnownLocationSettings.DEFAULT_MAX_STORED_LOCATIONS,
                        1),
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, KnownLocationEvaluatorConfig.FIRST_LOCATION_SCORE_SETTING_KEY,
                        "First tracked location score",
                        "Risk score when the user has no known locations yet.",
                        KnownLocationEvaluatorConfig.DEFAULT_FIRST_LOCATION_SCORE),
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, KnownLocationEvaluatorConfig.KNOWN_LOCATION_SCORE_SETTING_KEY,
                        "Known location score",
                        "Risk score when city and country match a known location (trust signal).",
                        KnownLocationEvaluatorConfig.DEFAULT_KNOWN_LOCATION_SCORE),
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, KnownLocationEvaluatorConfig.SAME_COUNTRY_SCORE_SETTING_KEY,
                        "Same country score",
                        "Risk score when the country was seen before but the city is new.",
                        KnownLocationEvaluatorConfig.DEFAULT_SAME_COUNTRY_SCORE),
                EvaluatorSettingProperties.scoreProperty(
                        evaluatorClass, KnownLocationEvaluatorConfig.NEW_COUNTRY_SCORE_SETTING_KEY,
                        "New country score",
                        "Risk score when the login country was never seen before for this user.",
                        KnownLocationEvaluatorConfig.DEFAULT_NEW_COUNTRY_SCORE));
    }
}
