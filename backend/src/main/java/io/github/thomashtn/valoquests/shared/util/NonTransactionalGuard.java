package io.github.thomashtn.valoquests.shared.util;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Guards workflows that must run outside a database transaction.
 *
 * <p>Such workflows commit their progress step by step so a crash keeps what was done; an enclosing
 * transaction would defer every commit to the end and silently defeat that.
 */
public final class NonTransactionalGuard {

    /**
     * Not instantiable: static helpers only.
     */
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
