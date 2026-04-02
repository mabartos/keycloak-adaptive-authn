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
package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.UserContextCondition;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.List;

/**
 * Condition for checking location properties
 */
public class LocationCondition implements UserContextCondition, ConditionalAuthenticator {
    private static final Logger log = Logger.getLogger(LocationCondition.class);
    private final LocationContext locationContext;
    private final List<Operation<LocationContext>> rules;

    public LocationCondition(KeycloakSession session, List<Operation<LocationContext>> rules) {
        this.locationContext = UserContexts.getContext(session, LocationContext.class);
        this.rules = rules;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        log.tracef("[matchCondition()] start");

        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig == null) {
            log.tracef("[matchCondition()] authConfig is null, return false");
            return false;
        }

        var config = authConfig.getConfig();

        // Country is required
        var countryOperation = config.get(LocationConditionFactory.COUNTRY_LIST_CONFIG);
        var countryValue = config.get(LocationConditionFactory.COUNTRY_VALUE_CONFIG);

        log.tracef("[matchCondition()] countryOperation=%s & countryValue=%s", countryOperation, countryValue);

        if (StringUtil.isBlank(countryOperation) || StringUtil.isBlank(countryValue)) {
            log.tracef("[matchCondition()] countryOperation or countryValue is blank, return false");
            return false;
        }

        // Check country condition
        boolean countryMatches = rules.stream()
                .filter(f -> f.getText().equals(countryOperation))
                .allMatch(f -> f.match(context.getRealm(), locationContext, countryValue));
        log.tracef("[matchCondition()] countryMatches result: %s", countryMatches);

        if (!countryMatches) {
            return false;
        }

        // City is optional - if specified, it must also match
        var cityOperation = config.get(LocationConditionFactory.CITY_LIST_CONFIG);
        var cityValue = config.get(LocationConditionFactory.CITY_VALUE_CONFIG);

        log.tracef("[matchCondition()] cityOperation=%s & cityValue=%s", cityOperation, cityValue);

        if (StringUtil.isNotBlank(cityOperation) && StringUtil.isNotBlank(cityValue)) {
            boolean cityMatches = rules.stream()
                    .filter(f -> f.getText().equals(cityOperation))
                    .allMatch(f -> f.match(context.getRealm(), locationContext, cityValue));

            log.tracef("[matchCondition()] cityMatches result: %s", cityMatches);
            return cityMatches;
        }

        log.tracef("[matchCondition()] cityOperation or cityValue is blank, return true");
        return true;
    }
}
