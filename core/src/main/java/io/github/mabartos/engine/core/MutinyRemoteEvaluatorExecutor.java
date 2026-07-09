package io.github.mabartos.engine.core;

import io.github.mabartos.spi.engine.RemoteEvaluationContext;
import io.github.mabartos.spi.engine.RemoteEvaluatorExecutor;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Remote evaluator executor using SmallRye Mutiny reactive streams.
 * <p>
 * Evaluators run asynchronously on the {@code risk-engine} executor thread pool.
 */
public class MutinyRemoteEvaluatorExecutor implements RemoteEvaluatorExecutor {
    private static final Logger logger = Logger.getLogger(MutinyRemoteEvaluatorExecutor.class);

    private final ExecutorService executor;

    public MutinyRemoteEvaluatorExecutor(KeycloakSession session) {
        this.executor = session.getProvider(ExecutorsProvider.class).getExecutor("risk-engine");
    }

    @Override
    public RemoteEvaluation start(RemoteEvaluationContext context) {
        CompletableFuture<Set<RiskEvaluator>> future = Multi.createFrom()
                .items(context.evaluators().stream())
                .onItem()
                .transformToUniAndMerge(evaluator ->
                        Uni.createFrom()
                                .item(evaluator)
                                .onItem()
                                .invoke(eval -> context.callback().execute(eval, context.realm(), context.knownUser()))
                                .emitOn(executor)
                                .onFailure()
                                .recoverWithUni(Uni.createFrom().nothing())
                                .ifNoItem()
                                .after(context.timeout())
                                .recoverWithUni(Uni.createFrom().nothing())
                )
                .filter(Objects::nonNull)
                .collect()
                .asSet()
                .subscribeAsCompletionStage();

        return () -> {
            try {
                return future.get(context.timeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warnf(e, "Remote evaluator execution failed");
                return Set.of();
            }
        };
    }
}
