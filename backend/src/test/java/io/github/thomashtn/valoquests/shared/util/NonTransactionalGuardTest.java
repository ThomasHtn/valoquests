package io.github.thomashtn.valoquests.shared.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link NonTransactionalGuard}.
 */
class NonTransactionalGuardTest {

    /**
     * Clears the actual-transaction-active flag so a failure here never leaks into another test
     * running on the same thread.
     */
    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    /**
     * Verifies that the guard is silent outside a transaction.
     */
    @Test
    void shouldDoNothingWhenNoTransactionIsActive() {
        assertThatCode(() -> NonTransactionalGuard.assertNoActiveTransaction("Player synchronization"))
            .doesNotThrowAnyException();
    }

    /**
     * Verifies that the guard fails fast when a transaction is active, naming the protected
     * workflow in the failure message.
     */
    @Test
    void shouldThrowWhenATransactionIsActive() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        assertThatThrownBy(() -> NonTransactionalGuard.assertNoActiveTransaction("Player synchronization"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Player synchronization");
    }
}
