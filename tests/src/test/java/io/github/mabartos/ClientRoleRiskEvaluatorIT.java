package io.github.mabartos;

import io.github.mabartos.evaluator.client.ClientRoleRiskEvaluator;
import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = ClientRoleRiskEvaluatorIT.Config.class)
class ClientRoleRiskEvaluatorIT {

    private static final String TEST_CLIENT_ID = "risk-role-client";

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void evaluate_returnsConfiguredScoreForMappedClientRole() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            ClientModel client = requireTestClient(session, realm);
            UserModel user = requireUser(session, realm);
            var roleSnapshot = RoleAttributeSnapshot.capture(client);
            var userRoleSnapshot = UserRoleSnapshot.capture(user, client);
            try {
                setRoleScore(client, "admin", Risk.Score.HIGH);
                grantClientRole(user, client, "admin");

                withAuthSession(session, realm, client, () -> {
                    Risk risk = new ClientRoleRiskEvaluator(session).evaluate(realm, user);
                    assertThat(risk.getScore(), is(Risk.Score.HIGH));
                    assertThat(risk.getReason().orElse(""), containsString("admin"));
                });
            } finally {
                userRoleSnapshot.restore(user, client);
                roleSnapshot.restore(client);
            }
        });
    }

    @Test
    void evaluate_returnsInvalidWhenNoActiveRoleAttributes() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            ClientModel client = requireTestClient(session, realm);
            UserModel user = requireUser(session, realm);
            var roleSnapshot = RoleAttributeSnapshot.capture(client);
            var userRoleSnapshot = UserRoleSnapshot.capture(user, client);
            try {
                clearRoleRiskScores(client);
                grantClientRole(user, client, "admin");

                withAuthSession(session, realm, client, () -> {
                    Risk risk = new ClientRoleRiskEvaluator(session).evaluate(realm, user);
                    assertThat(risk.getScore(), is(Risk.Score.INVALID));
                    assertThat(risk.getReason().orElse(""), containsString("No active client role risk attributes"));
                });
            } finally {
                userRoleSnapshot.restore(user, client);
                roleSnapshot.restore(client);
            }
        });
    }

    @Test
    void evaluate_returnsNegativeLowWhenUserHasNoClientRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            ClientModel client = requireTestClient(session, realm);
            UserModel user = requireUser(session, realm);
            var roleSnapshot = RoleAttributeSnapshot.capture(client);
            var userRoleSnapshot = UserRoleSnapshot.capture(user, client);
            try {
                setRoleScore(client, "admin", Risk.Score.HIGH);
                revokeAllClientRoles(user, client);

                withAuthSession(session, realm, client, () -> {
                    Risk risk = new ClientRoleRiskEvaluator(session).evaluate(realm, user);
                    assertThat(risk.getScore(), is(Risk.Score.NEGATIVE_LOW));
                });
            } finally {
                userRoleSnapshot.restore(user, client);
                roleSnapshot.restore(client);
            }
        });
    }

    @Test
    void evaluate_usesMaxScoreAcrossMappedRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            ClientModel client = requireTestClient(session, realm);
            UserModel user = requireUser(session, realm);
            var roleSnapshot = RoleAttributeSnapshot.capture(client);
            var userRoleSnapshot = UserRoleSnapshot.capture(user, client);
            try {
                setRoleScore(client, "admin", Risk.Score.HIGH);
                setRoleScore(client, "viewer", Risk.Score.VERY_SMALL);
                grantClientRole(user, client, "admin");
                grantClientRole(user, client, "viewer");

                withAuthSession(session, realm, client, () -> {
                    Risk risk = new ClientRoleRiskEvaluator(session).evaluate(realm, user);
                    assertThat(risk.getScore(), is(Risk.Score.HIGH));
                });
            } finally {
                userRoleSnapshot.restore(user, client);
                roleSnapshot.restore(client);
            }
        });
    }

    @Test
    void evaluate_returnsInvalidWhenAssignedRoleHasNoScoreAttribute() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            ClientModel client = requireTestClient(session, realm);
            UserModel user = requireUser(session, realm);
            var roleSnapshot = RoleAttributeSnapshot.capture(client);
            var userRoleSnapshot = UserRoleSnapshot.capture(user, client);
            try {
                setRoleScore(client, "admin", Risk.Score.HIGH);
                grantClientRole(user, client, "viewer");

                withAuthSession(session, realm, client, () -> {
                    Risk risk = new ClientRoleRiskEvaluator(session).evaluate(realm, user);
                    assertThat(risk.getScore(), is(Risk.Score.INVALID));
                    assertThat(risk.getReason().orElse(""), containsString("viewer"));
                });
            } finally {
                userRoleSnapshot.restore(user, client);
                roleSnapshot.restore(client);
            }
        });
    }

    @Test
    void evaluate_returnsInvalidWhenAssignedRoleHasExplicitNoneOnly() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            ClientModel client = requireTestClient(session, realm);
            UserModel user = requireUser(session, realm);
            var roleSnapshot = RoleAttributeSnapshot.capture(client);
            var userRoleSnapshot = UserRoleSnapshot.capture(user, client);
            try {
                setRoleScore(client, "admin", Risk.Score.HIGH);
                setRoleScore(client, "viewer", Risk.Score.NONE);
                grantClientRole(user, client, "viewer");

                withAuthSession(session, realm, client, () -> {
                    Risk risk = new ClientRoleRiskEvaluator(session).evaluate(realm, user);
                    assertThat(risk.getScore(), is(Risk.Score.INVALID));
                    assertThat(risk.getReason().orElse(""), containsString("viewer"));
                });
            } finally {
                userRoleSnapshot.restore(user, client);
                roleSnapshot.restore(client);
            }
        });
    }

    private static ClientModel requireTestClient(KeycloakSession session, RealmModel realm) {
        ClientModel client = session.clients().getClientByClientId(realm, TEST_CLIENT_ID);
        if (client == null) {
            client = session.clients().addClient(realm, TEST_CLIENT_ID);
            client.setEnabled(true);
            client.setPublicClient(true);
            client.setDirectAccessGrantsEnabled(true);
            client.setProtocol("openid-connect");
        }
        requireClientRole(client, "admin");
        requireClientRole(client, "viewer");
        return client;
    }

    private static RoleModel requireClientRole(ClientModel client, String roleName) {
        RoleModel role = client.getRole(roleName);
        if (role == null) {
            role = client.addRole(roleName);
        }
        return role;
    }

    private static UserModel requireUser(KeycloakSession session, RealmModel realm) {
        UserModel user = session.users().getUserByUsername(realm, "user");
        assertThat("Expected test user in adaptive realm", user != null, is(true));
        return user;
    }

    private static void grantClientRole(UserModel user, ClientModel client, String roleName) {
        user.grantRole(requireClientRole(client, roleName));
    }

    private static void revokeAllClientRoles(UserModel user, ClientModel client) {
        user.getClientRoleMappingsStream(client).forEach(user::deleteRoleMapping);
    }

    private static void withAuthSession(
            KeycloakSession session, RealmModel realm, ClientModel client, Runnable action) {
        RootAuthenticationSessionModel root = session.authenticationSessions().createRootAuthenticationSession(realm);
        try {
            AuthenticationSessionModel authSession = root.createAuthenticationSession(client);
            session.getContext().setRealm(realm);
            session.getContext().setAuthenticationSession(authSession);
            action.run();
        } finally {
            session.authenticationSessions().removeRootAuthenticationSession(realm, root);
        }
    }

    private static void setRoleScore(ClientModel client, String roleName, Risk.Score score) {
        requireClientRole(client, roleName).setSingleAttribute(
                ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE, score.name());
    }

    private static void clearRoleRiskScores(ClientModel client) {
        client.getRolesStream().forEach(role -> role.removeAttribute(ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE));
    }

    private static final class RoleAttributeSnapshot {
        private final Map<String, String> roleScores;

        private RoleAttributeSnapshot(Map<String, String> roleScores) {
            this.roleScores = roleScores;
        }

        static RoleAttributeSnapshot capture(ClientModel client) {
            Map<String, String> scores = new HashMap<>();
            client.getRolesStream().forEach(role -> {
                String score = role.getFirstAttribute(ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE);
                if (score != null) {
                    scores.put(role.getName(), score);
                }
            });
            return new RoleAttributeSnapshot(scores);
        }

        void restore(ClientModel client) {
            client.getRolesStream().forEach(role -> role.removeAttribute(ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE));
            roleScores.forEach((roleName, score) ->
                    requireClientRole(client, roleName).setSingleAttribute(
                            ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE, score));
        }
    }

    private static final class UserRoleSnapshot {
        private final Set<String> roleNames;

        private UserRoleSnapshot(Set<String> roleNames) {
            this.roleNames = roleNames;
        }

        static UserRoleSnapshot capture(UserModel user, ClientModel client) {
            Set<String> names = user.getClientRoleMappingsStream(client)
                    .map(RoleModel::getName)
                    .collect(Collectors.toCollection(HashSet::new));
            return new UserRoleSnapshot(names);
        }

        void restore(UserModel user, ClientModel client) {
            revokeAllClientRoles(user, client);
            roleNames.forEach(roleName -> grantClientRole(user, client, roleName));
        }
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("features", "declarative-ui");
        }
    }
}
