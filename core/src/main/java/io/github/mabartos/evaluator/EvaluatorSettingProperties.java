package io.github.mabartos.evaluator;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.Risk;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Builders for {@link ProviderConfigProperty} entries declared in
 * {@link RiskEvaluatorFactory#getAdditionalAdminConfigProperties()}.
 */
public final class EvaluatorSettingProperties {

    private EvaluatorSettingProperties() {
    }

    public static List<ProviderConfigProperty> of(ProviderConfigProperty... properties) {
        return Arrays.asList(properties);
    }

    public static ProviderConfigProperty scoreProperty(
            Class<? extends RiskEvaluator> evaluatorClass,
            String settingKey,
            String label,
            String helpText,
            Risk.Score defaultValue) {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey))
                .label(label)
                .helpText(helpText)
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(EvaluatorSettingUtils.configurableScoreNames())
                .defaultValue(defaultValue.name())
                .add()
                .build()
                .getFirst();
    }

    public static ProviderConfigProperty intProperty(
            Class<? extends RiskEvaluator> evaluatorClass,
            String settingKey,
            String label,
            String helpText,
            int defaultValue,
            int minimum) {
        return new EvaluatorIntegerConfigProperty(
                RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey),
                label,
                helpText,
                defaultValue,
                minimum);
    }

    public static ProviderConfigProperty stringProperty(
            Class<? extends RiskEvaluator> evaluatorClass,
            String settingKey,
            String label,
            String helpText) {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey))
                .label(label)
                .helpText(helpText)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build()
                .getFirst();
    }
}
