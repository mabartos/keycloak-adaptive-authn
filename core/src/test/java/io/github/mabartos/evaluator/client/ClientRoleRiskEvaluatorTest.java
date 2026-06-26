package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

class ClientRoleRiskEvaluatorTest {

    @Test
    void noAssignedRolesReturnsNegativeLow() {
        Risk risk = ClientRoleRiskEvaluator.evaluateAssignments("grafana", Map.of());
        assertThat(risk.getScore(), is(Risk.Score.NEGATIVE_LOW));
    }

    @Test
    void configuredRoleReturnsConfiguredScore() {
        Risk risk = ClientRoleRiskEvaluator.evaluateAssignments(
                "grafana",
                Map.of("admin", Optional.of(Risk.Score.HIGH)));
        assertThat(risk.getScore(), is(Risk.Score.HIGH));
    }

    @Test
    void soleUnconfiguredAssignedRoleReturnsInvalid() {
        Risk risk = ClientRoleRiskEvaluator.evaluateAssignments(
                "grafana",
                Map.of("viewer", Optional.empty()));
        assertThat(risk.getScore(), is(Risk.Score.INVALID));
        assertThat(risk.getReason().orElse(""), containsString("viewer"));
        assertThat(risk.getReason().orElse(""), containsString(ClientRoleRiskEvaluator.RISK_SCORE_ATTRIBUTE));
    }

    @Test
    void explicitNoneAssignedRoleReturnsInvalid() {
        Risk risk = ClientRoleRiskEvaluator.evaluateAssignments(
                "grafana",
                Map.of("viewer", Optional.of(Risk.Score.NONE)));
        assertThat(risk.getScore(), is(Risk.Score.INVALID));
        assertThat(risk.getReason().orElse(""), containsString("viewer"));
    }

    @Test
    void partiallyConfiguredRolesUsesHighestScorableScore() {
        Map<String, Optional<Risk.Score>> assigned = new LinkedHashMap<>();
        assigned.put("viewer", Optional.of(Risk.Score.NONE));
        assigned.put("editor", Optional.empty());
        assigned.put("admin", Optional.of(Risk.Score.HIGH));

        Risk risk = ClientRoleRiskEvaluator.evaluateAssignments("grafana", assigned);
        assertThat(risk.getScore(), is(Risk.Score.HIGH));
    }

    @Test
    void scoreFromRole_returnsEmptyWhenNoAttribute() {
        RoleModel role = role("admin", null);

        assertThat(ClientRoleRiskEvaluator.scoreFromRole(role), is(Optional.empty()));
    }

    @Test
    void scoreFromRole_parsesConfiguredScore() {
        RoleModel role = role("admin", "negative_low");

        assertThat(ClientRoleRiskEvaluator.scoreFromRole(role), is(Optional.of(Risk.Score.NEGATIVE_LOW)));
    }

    @Test
    void scoreFromRole_parsesExplicitNone() {
        RoleModel role = role("viewer", "NONE");

        assertThat(ClientRoleRiskEvaluator.scoreFromRole(role), is(Optional.of(Risk.Score.NONE)));
    }

    @Test
    void scoreFromRole_skipsInvalidScoreAtRuntime() {
        RoleModel role = role("admin", "NOT_A_SCORE");

        assertThat(ClientRoleRiskEvaluator.scoreFromRole(role), is(Optional.empty()));
    }

    @Test
    void getAttributeState_activeWhenRoleHasExplicitNone() {
        ClientModel client = clientWithRoles(Map.of("viewer", "NONE"));

        ClientRoleRiskEvaluator.AttributeState state = ClientRoleRiskEvaluator.getAttributeState(client);
        assertThat(state.configured(), is(true));
        assertThat(state.active(), is(true));
    }

    @Test
    void getAttributeState_emptyWhenClientHasNoRoles() {
        ClientModel client = clientWithRoles(Map.of());

        assertThat(ClientRoleRiskEvaluator.getAttributeState(client), is(ClientRoleRiskEvaluator.AttributeState.EMPTY));
    }

    @Test
    void getAttributeState_unusableWhenAttributesExistButInvalid() {
        ClientModel client = clientWithRoles(Map.of("admin", "NOT_A_SCORE"));

        ClientRoleRiskEvaluator.AttributeState state = ClientRoleRiskEvaluator.getAttributeState(client);
        assertThat(state.configured(), is(true));
        assertThat(state.active(), is(false));
        assertThat(state.unusable(), is(true));
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

    private static ClientModel clientWithRoles(Map<String, String> roleScores) {
        return (ClientModel) Proxy.newProxyInstance(
                ClientModel.class.getClassLoader(),
                new Class[]{ClientModel.class},
                (proxy, method, args) -> {
                    if ("getRolesStream".equals(method.getName())) {
                        return roleScores.entrySet().stream()
                                .map(entry -> role(entry.getKey(), entry.getValue()));
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
