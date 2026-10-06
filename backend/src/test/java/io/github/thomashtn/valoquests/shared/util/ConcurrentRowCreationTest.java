package io.github.thomashtn.valoquests.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link ConcurrentRowCreation}.
 */
class ConcurrentRowCreationTest {

    @Test
    @DisplayName("Returns the stored row without creating one")
    void shouldReturnTheStoredRow() {
        AtomicInteger creations = new AtomicInteger();

        String row = ConcurrentRowCreation.findOrCreate(
            () -> Optional.of("stored"),
            () -> "created" + creations.incrementAndGet()
        );

        assertThat(row).isEqualTo("stored");
        assertThat(creations).hasValue(0);
    }

    @Test
    @DisplayName("Creates the row when none is stored")
    void shouldCreateAMissingRow() {
        String row = ConcurrentRowCreation.findOrCreate(Optional::empty, () -> "created");

        assertThat(row).isEqualTo("created");
    }

    @Test
    @DisplayName("Reuses the row a concurrent creation committed when its own insert loses the race")
    void shouldReuseTheWinnersRowAfterLosingTheRace() {
        AtomicInteger lookups = new AtomicInteger();

        String row = ConcurrentRowCreation.findOrCreate(
            () -> lookups.incrementAndGet() == 1 ? Optional.empty() : Optional.of("winner"),
            () -> {
                throw new DataIntegrityViolationException("duplicate key");
            }
        );

        assertThat(row).isEqualTo("winner");
    }

    @Test
    @DisplayName("Rethrows the violation when no row explains it")
    void shouldRethrowAViolationNoRowExplains() {
        assertThatThrownBy(() -> ConcurrentRowCreation.findOrCreate(
            Optional::empty,
            () -> {
                throw new DataIntegrityViolationException("not null violated");
            }
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Refuses to run inside a transaction, which a lost race would poison")
    void shouldRefuseToRunInsideATransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        try {
            assertThatThrownBy(() -> ConcurrentRowCreation.findOrCreate(
                Optional::empty,
                () -> "created"
            )).isInstanceOf(IllegalStateException.class);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
