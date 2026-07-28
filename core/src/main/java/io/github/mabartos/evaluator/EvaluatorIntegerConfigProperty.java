package io.github.mabartos.evaluator;

import org.keycloak.provider.ProviderConfigProperty;

/**
 * Integer evaluator setting with an explicit validation minimum declared at definition time.
 */
public final class EvaluatorIntegerConfigProperty extends ProviderConfigProperty {

    private final int minimum;

    public EvaluatorIntegerConfigProperty(
            String name, String label, String helpText, int defaultValue, int minimum) {
        super(name, label, helpText, INTEGER_TYPE, defaultValue);
        this.minimum = minimum;
    }

    public int getMinimum() {
        return minimum;
    }
}
