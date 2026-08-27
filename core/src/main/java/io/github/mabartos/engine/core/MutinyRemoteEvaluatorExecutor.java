package io.github.mabartos.engine.core;

import io.github.mabartos.spi.engine.RemoteEvaluationContext;
import io.github.mabartos.spi.engine.RemoteEvaluatorExecutor;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

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
        var sessionFactory = context.session().getKeycloakSessionFactory();
        var sessionContext = context.session().getContext();
        var realmId = context.realm().getId();
        var userId = context.knownUser() != null ? context.knownUser().getId() : null;

        CompletableFuture<Set<RiskEvaluator>> future = Multi.createFrom()
                .items(context.evaluators().stream())
                .onItem()
                .transformToUniAndMerge(evaluator ->
                        Uni.createFrom()
                                .item(evaluator)
                                .emitOn(executor)
                                .onItem()
                                .invoke(eval -> KeycloakModelUtils.runJobInTransactionWithResult(
                                        sessionFactory, sessionContext, s -> {
                                            var freshRealm = s.realms().getRealm(realmId);
                                            var freshUser = userId != null
                                                    ? s.users().getUserById(freshRealm, userId)
                                                    : null;
                                            context.callback().execute(eval, freshRealm, freshUser);
                                            return null;
                                        }, "MutinyRemoteEvaluatorExecutor.execute"))
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
                var completed = future.get(context.timeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                return completed.stream()
                        .filter(e -> e.getRisk().isValid())
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                logger.warnf(e, "Remote evaluator execution failed");
                return Set.of();
            }
        };
    }
}
