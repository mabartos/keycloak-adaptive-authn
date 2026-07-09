package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RoleModel;

import java.lang.reflect.Proxy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ClientRoleRiskEvaluatorTest {

    @Test
    void scoreFromRoleName_managePrefixReturnsMedium() {
        assertThat(ClientRoleRiskEvaluator.scoreFromRoleName("manage-users"), is(Risk.Score.MEDIUM));
    }

    @Test
    void scoreFromRoleName_createPrefixReturnsSmall() {
        assertThat(ClientRoleRiskEvaluator.scoreFromRoleName("create-client"), is(Risk.Score.SMALL));
    }

    @Test
    void scoreFromRoleName_viewPrefixReturnsNone() {
        assertThat(ClientRoleRiskEvaluator.scoreFromRoleName("view-users"), is(Risk.Score.NONE));
    }

    @Test
    void scoreFromRoleName_unknownRoleReturnsNone() {
        assertThat(ClientRoleRiskEvaluator.scoreFromRoleName("admin"), is(Risk.Score.NONE));
    }

    @Test
    void scoreFromRoleName_impersonationReturnsMedium() {
        assertThat(ClientRoleRiskEvaluator.scoreFromRoleName("impersonation"), is(Risk.Score.MEDIUM));
    }

    @Test
    void scoreForRole_usesPrefixWhenNoAttribute() {
        RoleModel role = role("manage-reports", null);

        assertThat(ClientRoleRiskEvaluator.scoreForRole(role), is(Risk.Score.MEDIUM));
    }

    @Test
    void scoreForRole_attributeOverridesPrefix() {
        RoleModel role = role("manage-reports", "NONE");

        assertThat(ClientRoleRiskEvaluator.scoreForRole(role), is(Risk.Score.NONE));
    }

    @Test
    void scoreForRole_attributeOverridesPrefixWithHigherScore() {
        RoleModel role = role("viewer", "HIGH");

        assertThat(ClientRoleRiskEvaluator.scoreForRole(role), is(Risk.Score.HIGH));
    }

    @Test
    void parseAttributeScore_returnsEmptyWhenNoAttribute() {
        RoleModel role = role("admin", null);

        assertThat(ClientRoleRiskEvaluator.parseAttributeScore(role).isEmpty(), is(true));
    }

    @Test
    void parseAttributeScore_parsesConfiguredScore() {
        RoleModel role = role("admin", "negative_low");

        assertThat(ClientRoleRiskEvaluator.parseAttributeScore(role), is(java.util.Optional.of(Risk.Score.NEGATIVE_LOW)));
    }

    @Test
    void parseAttributeScore_parsesExplicitNone() {
        RoleModel role = role("viewer", "NONE");

        assertThat(ClientRoleRiskEvaluator.parseAttributeScore(role), is(java.util.Optional.of(Risk.Score.NONE)));
    }

    @Test
    void parseAttributeScore_skipsInvalidScoreAtRuntime() {
        RoleModel role = role("admin", "NOT_A_SCORE");

        assertThat(ClientRoleRiskEvaluator.parseAttributeScore(role).isEmpty(), is(true));
    }

    @Test
    void scoreForRole_fallsBackToPrefixWhenAttributeInvalid() {
        RoleModel role = role("manage-reports", "NOT_A_SCORE");

        assertThat(ClientRoleRiskEvaluator.scoreForRole(role), is(Risk.Score.MEDIUM));
    }

    private static RoleModel role(String name, String scoreRaw) {
        return (RoleModel) Proxy.newProxyInstance(
                RoleModel.class.getClassLoader(),
                new Class[]{RoleModel.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return name;
                    }
                    if ("getFirstAttribute".equals(method.getName())
                            && args.length == 1
                            && ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE.equals(args[0])) {
                        return scoreRaw;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }
}
