package io.github.mabartos.ui;

import io.github.mabartos.evaluator.EvaluatorUtils;
import io.github.mabartos.evaluator.browser.BrowserRiskEvaluator;
import io.github.mabartos.evaluator.browser.BrowserRiskEvaluatorFactory;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.RealmModel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.mabartos.spi.evaluator.RiskEvaluatorFactory.getTrustConfig;
import static io.github.mabartos.spi.evaluator.RiskEvaluatorFactory.isEnabledConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskBasedPoliciesUiTabPersistenceTest {

    private final Map<String, String> realmAttributes = new HashMap<>();
    private RealmModel realm;
    private RiskBasedPoliciesUiTab tab;
    private BrowserRiskEvaluatorFactory browserFactory;

    @BeforeEach
    void setUp() throws Exception {
        realmAttributes.clear();
        realm = realmBackedBy(realmAttributes);
        tab = new RiskBasedPoliciesUiTab();
        browserFactory = new BrowserRiskEvaluatorFactory();
        injectFactories(tab, List.of(browserFactory));
    }

    @Test
    void onCreate_disablingEvaluatorPersistsFalse() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        var model = componentModel(Map.of(enabledKey, "false"));

        tab.onCreate(null, realm, model);

        assertFalse(EvaluatorUtils.isEvaluatorEnabled(realm, BrowserRiskEvaluator.class));
    }

    @Test
    void onUpdate_disablingEvaluatorWhenKeyPresent() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        EvaluatorUtils.setEvaluatorEnabled(realm, BrowserRiskEvaluator.class, true);

        var oldModel = componentModel(Map.of(enabledKey, "true"));
        var newModel = componentModel(Map.of(enabledKey, "false"));

        tab.onUpdate(null, realm, oldModel, newModel);

        assertFalse(EvaluatorUtils.isEvaluatorEnabled(realm, BrowserRiskEvaluator.class));
    }

    @Test
    void onUpdate_skipsEnabledWhenKeyAbsentFromNewModel() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        EvaluatorUtils.setEvaluatorEnabled(realm, BrowserRiskEvaluator.class, true);

        var oldModel = componentModel(Map.of(enabledKey, "true"));
        var newModel = new ComponentModel();
        newModel.getConfig().putSingle(getTrustConfig(BrowserRiskEvaluator.class), "0.5");

        tab.onUpdate(null, realm, oldModel, newModel);

        assertTrue(EvaluatorUtils.isEvaluatorEnabled(realm, BrowserRiskEvaluator.class));
    }

    @Test
    void onCreate_storesMasterSwitchFalse() {
        var model = componentModel(Map.of(RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG, "false"));

        tab.onCreate(null, realm, model);

        assertEquals("false", realmAttributes.get(RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG));
    }

    @Test
    void validateConfiguration_rejectsTrustAboveOne() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var model = componentModel(Map.of(trustKey, "1.01"));

        assertThrows(ComponentValidationException.class,
                () -> tab.validateConfiguration(null, realm, model));
    }

    @Test
    void validateConfiguration_acceptsValidTrust() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var model = componentModel(Map.of(trustKey, "0.75"));

        tab.validateConfiguration(null, realm, model);
    }

    private static ComponentModel componentModel(Map<String, String> entries) {
        var model = new ComponentModel();
        entries.forEach((key, value) -> model.getConfig().putSingle(key, value));
        return model;
    }

    private static void injectFactories(RiskBasedPoliciesUiTab tab, List<RiskEvaluatorFactory> factories)
            throws Exception {
        setField(tab, "riskEvaluatorFactories", factories);
        setField(tab, "algorithmFactories", List.of());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static RealmModel realmBackedBy(Map<String, String> attributes) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] {RealmModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        attributes.put((String) args[0], (String) args[1]);
                        yield null;
                    }
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "getId" -> "test-realm";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == Optional.class) {
            return Optional.empty();
        }
        return null;
    }
}
