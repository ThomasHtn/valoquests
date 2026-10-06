package io.github.thomashtn.valoquests.shared.util;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Finds a row by its unique key or creates it, tolerating a concurrent creation of the same row.
 *
 * <p>The unique constraint settles the race and the loser reloads the winner's row. Must run outside a
 * transaction, which the failed insert would mark rollback-only.
 */
public final class ConcurrentRowCreation {

    /**
     * Not instantiable: static helpers only.
     */
    private ConcurrentRowCreation() {
    }

    /**
     * Returns the existing row, or the one created by this call or by a concurrent one.
     *
     * @param find   lookup by the unique key
     * @param create insert of a new row, committing on its own
     * @param <T>    entity type
     * @return the stored row
     * @throws IllegalStateException when called inside a transaction
     */
    public static <T> T findOrCreate(Supplier<Optional<T>> find, Supplier<T> create) {
        NonTransactionalGuard.assertNoActiveTransaction("Concurrent row creation");

        return find.get().orElseGet(() -> {
            try {
                return create.get();
            } catch (DataIntegrityViolationException raceLost) {
                return find.get().orElseThrow(() -> raceLost);
            }
        });
    }
}
