package io.github.thomashtn.valoquests.shared.util;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Guards workflows that must run outside a database transaction.
 *
 * <p>Some workflows persist their own progress incrementally on purpose - a checkpoint, a
 * completion flag - so that a crash partway through leaves only what was actually committed
 * instead of losing an entire run to a rollback. Wrapping such a workflow in a transaction defers
 * every one of its commits to the end and silently defeats that guarantee. {@link
 * #assertNoActiveTransaction} turns the invariant into a runtime check instead of a comment a
 * future change can miss.
 */
public final class NonTransactionalGuard {

    private NonTransactionalGuard() {
    }

    /**
     * Fails fast when a database transaction is already active on the calling thread.
     *
     * @param context short description of the protected workflow, used in the failure message
     * @throws IllegalStateException when a transaction is active
     */
    public static void assertNoActiveTransaction(String context) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                context + " must not run inside a database transaction"
            );
        }
    }
}
