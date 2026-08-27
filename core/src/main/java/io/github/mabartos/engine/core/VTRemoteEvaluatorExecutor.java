package io.github.mabartos.engine.core;

import io.github.mabartos.spi.engine.RemoteEvaluationContext;
import io.github.mabartos.spi.engine.RemoteEvaluatorExecutor;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.jboss.logging.Logger;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Remote evaluator executor using virtual threads with {@link StructuredTaskScope}.
 * <p>
 * Each evaluator runs in its own virtual thread with a separate transaction
 * and fresh realm/user references from the Infinispan cache.
 * <p>
 * Requires Java 21+ with {@code --enable-preview}, or Java 23+ without it.
 */
public class VTRemoteEvaluatorExecutor implements RemoteEvaluatorExecutor {
    private static final Logger logger = Logger.getLogger(VTRemoteEvaluatorExecutor.class);

    @Override
    public RemoteEvaluation start(RemoteEvaluationContext context) {
        Set<RiskEvaluator> completedEvaluators = ConcurrentHashMap.newKeySet();
        var scope = new StructuredTaskScope<RiskEvaluator>();

        for (var evaluator : context.evaluators()) {
            scope.fork(() -> {
                try {
                    return KeycloakModelUtils.runJobInTransactionWithResult(
                            context.session().getKeycloakSessionFactory(),
                            context.session().getContext(),
                            s -> {
                                var freshRealm = s.realms().getRealm(context.realm().getId());
                                var freshUser = context.knownUser() != null
                                        ? s.users().getUserById(freshRealm, context.knownUser().getId())
                                        : null;

                                context.callback().execute(evaluator, freshRealm, freshUser);
                                completedEvaluators.add(evaluator);
                                return evaluator;
                            }, "VTRemoteEvaluatorExecutor.execute");
                } catch (Exception e) {
                    logger.warnf(e, "Remote evaluator %s failed with exception", evaluator.getClass().getSimpleName());
                    return null;
                }
            });
        }

        return () -> {
            try (scope) {
                try {
                    scope.joinUntil(Instant.now().plus(context.timeout()));
                    logger.debugf("Remote risk evaluation completed - %d/%d remote evaluators finished in time",
                            completedEvaluators.size(), context.evaluators().size());
                } catch (TimeoutException e) {
                    logger.warnf("Remote risk evaluation timeout exceeded: %d ms - %d/%d remote evaluators completed",
                            context.timeout().toMillis(), completedEvaluators.size(), context.evaluators().size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Risk evaluation was interrupted", e);
                }
            } catch (Exception e) {
                logger.error("Error during parallel remote risk evaluation", e);
            }

            return completedEvaluators.stream()
                    .filter(e -> e.getRisk().isValid())
                    .collect(Collectors.toSet());
        };
    }
}
