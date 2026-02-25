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

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.location.IpApiLocationContext;
import io.github.mabartos.context.location.IpApiLocationContextFactory;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Set;

import static io.github.mabartos.context.ip.client.TestIpAddressContextFactory.USE_TESTING_IP_PROP;

/**
 * Risk evaluator for location properties
 */
public class LocationRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(LocationRiskEvaluator.class);

    private final IpApiLocationContext locationContext;

    public LocationRiskEvaluator(KeycloakSession session) {
        this.locationContext = UserContexts.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var data = locationContext.getData(realm, knownUser).orElse(null);
        if (data != null) {
            logger.trace(data.toString());
            // TODO save location to successful logins and then compare it here
            //session.singleUseObjects().put(getUserLocationKey(user),);

            // TODO implement it properly
            if (Configuration.isTrue(USE_TESTING_IP_PROP)) {
                if (data.getCity().contains("Prague")) {
                    return Risk.of(Risk.MEDIUM, "Don't know, but requests from Prague are suspicious :P");
                }
            }

            return Risk.none();
        } else {
            logger.tracef("Data for LocationRiskEvaluator is null");
        }

        return Risk.invalid("Cannot obtain location information");
    }

    protected String getUserLocationKey(UserModel user) {
        return "location-" + user.getId();
    }
}
