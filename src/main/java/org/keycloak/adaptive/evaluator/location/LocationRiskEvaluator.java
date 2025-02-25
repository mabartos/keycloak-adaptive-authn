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
import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.location.IpApiLocationContextFactory;
import org.keycloak.adaptive.context.location.LocationContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;

/**
 * Risk evaluator for location properties
 */
public class LocationRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(LocationRiskEvaluator.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final LocationContext locationContext;

    public LocationRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.locationContext = UserContexts.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate() {
        if (realm == null) {
            logger.debugf("Realm is null");
            return Risk.invalid();
        }

        var user = session.getContext().getAuthenticationSession().getAuthenticatedUser();

        if (user == null) {
            logger.debugf("User is null");
            return Risk.invalid();
        }

        var data = locationContext.getData();
        if (data.isPresent()) {
            logger.debugf("Location - City: %s, Country: %s", data.get().getCity(), data.get().getCountry());

            // TODO save location to successful logins and then compare it here
            //session.singleUseObjects().put(getUserLocationKey(user),);

        } else {
            logger.debugf("Data for LocationRiskEvaluator is null");
        }

        return Risk.invalid();
    }

    protected String getUserLocationKey(UserModel user) {
        return "location-" + user.getId();
    }
}
