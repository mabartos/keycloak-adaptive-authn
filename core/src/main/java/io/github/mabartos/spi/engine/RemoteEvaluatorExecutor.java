package io.github.mabartos.spi.engine;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * Executor for running remote {@link RiskEvaluator}s asynchronously.
 * <p>
 * The {@link RiskEngine} delegates remote evaluator execution to this SPI,
 * allowing different concurrency strategies (virtual threads, reactive streams, etc.)
 * without coupling the engine to a specific implementation.
 * <p>
 * If no provider is available, the engine skips remote evaluators and logs a warning.
 */
public interface RemoteEvaluatorExecutor extends Provider {

    /**
     * Start remote evaluator execution and return immediately.
     * <p>
     * Implementations should run evaluators in parallel, each in its own transaction
     * with fresh realm/user references. Use {@link RemoteEvaluationContext#callback()} to
     * execute each evaluator — it wraps the engine's evaluation and tracing logic.
     * <p>
     * The returned {@link RemoteEvaluation} handle lets the engine run local evaluators
     * concurrently while remote I/O is in progress, then call {@link RemoteEvaluation#awaitResults()}
     * to collect the results.
     *
     * @param context the evaluation context containing evaluators, realm, user, timeout, session, and callback
     * @return a handle to await the results of the remote evaluation
     */
    RemoteEvaluation start(RemoteEvaluationContext context);

    /**
     * Whether local (non-remote) user contexts should be pre-initialized before evaluation.
     * <p>
     * Remote executors typically need this (default {@code true}) so contexts are warm
     * before evaluators run on separate threads. When no remote executor is available,
     * the engine skips pre-initialization.
     *
     * @return {@code true} if local user contexts should be eagerly initialized
     */
    default boolean requiresPreInitUserContexts() {
        return true;
    }

    @Override
    default void close() {
    }

    /**
     * Handle returned by {@link #start(RemoteEvaluationContext)} to collect results
     * after remote evaluators complete.
     */
    interface RemoteEvaluation {

        /**
         * Block until all remote evaluators finish (or timeout) and return the results.
         *
         * @return the set of evaluators that produced valid risk scores
         */
        Set<RiskEvaluator> awaitResults();
    }
}
