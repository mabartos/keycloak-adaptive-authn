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

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG;
import static io.github.mabartos.spi.engine.RiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;
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
        realm = realmBackedBy(realmAttributes, Map.of(), List.of());
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

    @Test
    void validateConfiguration_hydratesTrustFromRealmAttributeWhenModelEmpty() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(trustKey, "0.5");

        var model = new ComponentModel();
        tab.validateConfiguration(null, realm, model);

        assertEquals("0.5", model.get(trustKey));
    }

    @Test
    void validateConfiguration_hydratesEngineSettingFromRealmAttributeWhenModelEmpty() {
        realmAttributes.put(EVALUATOR_TIMEOUT_CONFIG, "5000");

        var model = new ComponentModel();
        tab.validateConfiguration(null, realm, model);

        assertEquals("5000", model.get(EVALUATOR_TIMEOUT_CONFIG));
    }

    @Test
    void validateConfiguration_prefersRealmTrustOverExistingModelValue() {
        realmAttributes.put(getTrustConfig(BrowserRiskEvaluator.class), "0.5");

        var model = componentModel(Map.of(getTrustConfig(BrowserRiskEvaluator.class), "0.75"));
        tab.validateConfiguration(null, realm, model);

        assertEquals("0.5", model.get(getTrustConfig(BrowserRiskEvaluator.class)));
    }

    @Test
    void validateConfiguration_appliesTrustDefaultWhenModelAndRealmEmpty() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var model = new ComponentModel();

        tab.validateConfiguration(null, realm, model);

        assertEquals("1.0", model.get(trustKey));
    }

    @Test
    void validateConfiguration_hydratesEnabledFalseFromRealmWhenModelEmpty() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(enabledKey, "false");

        var model = new ComponentModel();
        tab.validateConfiguration(null, realm, model);

        assertEquals("false", model.get(enabledKey));
    }

    @Test
    void validateConfiguration_overridesStaleModelEnabledWithRealmAttribute() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(enabledKey, "false");

        var model = componentModel(Map.of(enabledKey, "true"));
        tab.validateConfiguration(null, realm, model);

        assertEquals("false", model.get(enabledKey));
    }

    @Test
    void validateConfiguration_appliesEnabledDefaultWhenModelAndRealmEmpty() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        var model = new ComponentModel();

        tab.validateConfiguration(null, realm, model);

        assertEquals("true", model.get(enabledKey));
    }

    @Test
    void validateConfiguration_appliesEnabledDefaultWhenRealmAttributeBlank() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(enabledKey, "");

        var model = new ComponentModel();
        tab.validateConfiguration(null, realm, model);

        assertEquals("true", model.get(enabledKey));
    }

    @Test
    void validateConfiguration_appliesTrustDefaultWhenRealmAttributeBlank() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(trustKey, "");

        var model = new ComponentModel();
        tab.validateConfiguration(null, realm, model);

        assertEquals("1.0", model.get(trustKey));
    }

    @Test
    void validateConfiguration_discardsStaleModelTrustWhenNoRealmAttribute() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var persisted = tabComponent(Map.of(trustKey, "0.75"));
        persisted.setId("risk-tab");
        var model = componentModel(Map.of(trustKey, "0.75"));
        model.setId("risk-tab");
        realm = realmBackedBy(realmAttributes, Map.of("risk-tab", persisted), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("1.0", model.get(trustKey));
    }

    @Test
    void validateConfiguration_discardsStaleModelEnabledWhenNoRealmAttribute() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        var persisted = tabComponent(Map.of(enabledKey, "false"));
        persisted.setId("risk-tab");
        var model = componentModel(Map.of(enabledKey, "false"));
        model.setId("risk-tab");
        realm = realmBackedBy(realmAttributes, Map.of("risk-tab", persisted), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("true", model.get(enabledKey));
    }

    @Test
    void validateConfiguration_clearsStaleTrustWhenPersistedComponentUnknown() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var model = componentModel(Map.of(trustKey, "0.2"));

        tab.validateConfiguration(null, realm, model);

        assertEquals("1.0", model.get(trustKey));
    }

    @Test
    void validateConfiguration_clearsStaleTrustWhenModelHasNoComponentId() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var persisted = tabComponent(Map.of(trustKey, "0.2"));
        var model = componentModel(Map.of(trustKey, "0.2"));
        realm = realmBackedBy(realmAttributes, Map.of(), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("1.0", model.get(trustKey));
    }

    @Test
    void validateConfiguration_preservesFirstSaveTrustWhenModelHasNoComponentId() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var persisted = tabComponent(Map.of(RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG, "true"));
        var model = componentModel(Map.of(trustKey, "0.5"));
        realm = realmBackedBy(realmAttributes, Map.of(), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("0.5", model.get(trustKey));
    }

    @Test
    void validateConfiguration_preservesSubmittedTrustOnVirginRealmFirstSave() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        var persisted = tabComponent(Map.of(
                RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG, "true"));
        persisted.setId("risk-tab");
        var model = componentModel(Map.of(
                trustKey, "0.5",
                enabledKey, "true",
                RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG, "true"));
        model.setId("risk-tab");
        realm = realmBackedBy(realmAttributes, Map.of("risk-tab", persisted), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("0.5", model.get(trustKey));

        tab.onUpdate(null, realm, persisted, model);

        assertEquals("0.5", realmAttributes.get(trustKey));
    }

    @Test
    void onUpdate_persistsTrustToRealmEvenWhenUnchanged() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var model = componentModel(Map.of(trustKey, "0.5"));

        tab.onUpdate(null, realm, model, model);

        assertEquals("0.5", realmAttributes.get(trustKey));
    }

    @Test
    void validateConfiguration_preservesSubmittedTrustWhenRealmAttributeStillStale() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(trustKey, "0.75");
        var persisted = tabComponent(Map.of(trustKey, "0.75"));
        persisted.setId("risk-tab");
        var model = componentModel(Map.of(trustKey, "1.0"));
        model.setId("risk-tab");
        realm = realmBackedBy(realmAttributes, Map.of("risk-tab", persisted), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("1.0", model.get(trustKey));

        tab.onUpdate(null, realm, persisted, model);

        assertEquals("1.0", realmAttributes.get(trustKey));
    }

    @Test
    void validateConfiguration_preservesSubmittedEnabledWhenRealmAttributeStillStale() {
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        realmAttributes.put(enabledKey, "false");
        var persisted = tabComponent(Map.of(enabledKey, "false"));
        persisted.setId("risk-tab");
        var model = componentModel(Map.of(enabledKey, "true"));
        model.setId("risk-tab");
        realm = realmBackedBy(realmAttributes, Map.of("risk-tab", persisted), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("true", model.get(enabledKey));

        tab.onUpdate(null, realm, persisted, model);

        assertEquals("true", realmAttributes.get(enabledKey));
    }

    @Test
    void validateConfiguration_preservesSubmittedTimeoutWhenRealmAttributeStillStale() {
        realmAttributes.put(EVALUATOR_TIMEOUT_CONFIG, "2500");
        var persisted = tabComponent(Map.of(EVALUATOR_TIMEOUT_CONFIG, "2500"));
        persisted.setId("risk-tab");
        var model = componentModel(Map.of(EVALUATOR_TIMEOUT_CONFIG, "5000"));
        model.setId("risk-tab");
        realm = realmBackedBy(realmAttributes, Map.of("risk-tab", persisted), List.of(persisted));

        tab.validateConfiguration(null, realm, model);

        assertEquals("5000", model.get(EVALUATOR_TIMEOUT_CONFIG));

        tab.onUpdate(null, realm, persisted, model);

        assertEquals("5000", realmAttributes.get(EVALUATOR_TIMEOUT_CONFIG));
    }

    @Test
    void onUpdate_persistsEvaluatorSettingsFromFormAfterStaleComponentCleared() {
        var trustKey = getTrustConfig(BrowserRiskEvaluator.class);
        var enabledKey = isEnabledConfig(BrowserRiskEvaluator.class);
        var oldModel = componentModel(Map.of(trustKey, "0.75", enabledKey, "false"));
        var newModel = componentModel(Map.of(trustKey, "1.0", enabledKey, "true"));

        tab.onUpdate(null, realm, oldModel, newModel);

        assertEquals("1.0", realmAttributes.get(trustKey));
        assertEquals("true", realmAttributes.get(enabledKey));
    }

    @Test
    void collectEffectivePolicySettings_includesRealmAttributesAndDefaults() {
        realmAttributes.put(RISK_BASED_AUTHN_ENABLED_CONFIG, "false");
        var properties = tab.getConfigProperties();

        var snapshot = tab.collectEffectivePolicySettings(realm);

        assertEquals("false", snapshot.get(RISK_BASED_AUTHN_ENABLED_CONFIG));
        assertEquals(properties.size(), snapshot.size());
    }

    private static ComponentModel componentModel(Map<String, String> entries) {
        var model = new ComponentModel();
        entries.forEach((key, value) -> model.getConfig().putSingle(key, value));
        return model;
    }

    private static ComponentModel tabComponent(Map<String, String> entries) {
        var model = componentModel(entries);
        model.setProviderId("Risk-based policies");
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

    private static RealmModel realmBackedBy(
            Map<String, String> attributes,
            Map<String, ComponentModel> componentsById,
            List<ComponentModel> uiTabComponents) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] {RealmModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAttribute" -> {
                        attributes.put((String) args[0], (String) args[1]);
                        yield null;
                    }
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "getComponent" -> componentsById.get((String) args[0]);
                    case "getComponentsStream" -> {
                        if (args != null && args.length == 2
                                && "test-realm".equals(args[0])
                                && "org.keycloak.services.ui.extend.UiTabProvider".equals(args[1])) {
                            yield uiTabComponents.stream();
                        }
                        if (args != null && args.length == 1
                                && "org.keycloak.services.ui.extend.UiTabProvider".equals(args[0])) {
                            yield uiTabComponents.stream();
                        }
                        yield java.util.stream.Stream.<ComponentModel>empty();
                    }
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
