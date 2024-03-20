package org.keycloak.adaptive.spi.factor;

import org.keycloak.provider.ProviderFactory;

public interface RiskFactorEvaluatorFactory<T> extends ProviderFactory<RiskFactorEvaluator<T>> {
}
