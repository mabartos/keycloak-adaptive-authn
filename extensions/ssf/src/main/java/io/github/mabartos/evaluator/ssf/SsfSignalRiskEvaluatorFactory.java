package io.github.mabartos.evaluator.ssf;

import io.github.mabartos.context.ssf.SsfFeatureSupport;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

public class SsfSignalRiskEvaluatorFactory implements RiskEvaluatorFactory {

    public static final String PROVIDER_ID = "ssf-signal-risk-evaluator";
    private static final String NAME = "SSF Signal Risk Evaluator";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return SsfSignalRiskEvaluator.class;
    }

    @Override
    public SsfSignalRiskEvaluator create(KeycloakSession session) {
        return new SsfSignalRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return SsfFeatureSupport.isSsfAvailable();
    }
}
