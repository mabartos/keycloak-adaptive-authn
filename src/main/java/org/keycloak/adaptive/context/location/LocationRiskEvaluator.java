package org.keycloak.adaptive.context.location;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

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
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public double getWeight() {
        return Weight.DEFAULT;
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
