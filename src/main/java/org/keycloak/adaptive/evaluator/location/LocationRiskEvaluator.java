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
package org.keycloak.adaptive.evaluator.location;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.location.IpApiLocationContextFactory;
import org.keycloak.adaptive.context.location.LocationContext;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;

/**
 * Risk evaluator for location properties
 */
public class LocationRiskEvaluator implements RiskEvaluator {
    private static final Logger logger = Logger.getLogger(LocationRiskEvaluator.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final LocationContext locationContext;

    private Double risk;

    public LocationRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.locationContext = ContextUtils.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
    }

    @Override
    public Optional<Double> getRiskValue() {
        return Optional.ofNullable(risk);
    }

    @Override
    public double getWeight() {
        return Weight.DEFAULT;
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, LocationRiskEvaluatorFactory.class);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void evaluate() {
        if (realm == null) {
            logger.debugf("Realm is null");
            return;
        }

        var user = session.getContext().getAuthenticationSession().getAuthenticatedUser();

        if (user == null) {
            logger.debugf("User is null");
            return;
        }

        var data = locationContext.getData();
        if (data == null) {
            logger.debugf("Data for LocationRiskEvaluator is null");
            this.risk = null;
            return;
        }

        logger.debugf("Location - City: %s, Country: %s", data.getCity(), data.getCountry());

        // TODO save location to successful logins and then compare it here
        //session.singleUseObjects().put(getUserLocationKey(user),);
    }

    protected String getUserLocationKey(UserModel user) {
        return "location-" + user.getId();
    }
}
