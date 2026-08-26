package io.github.thomashtn.valoquests.colony.repository;

import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence operations for colony daily snapshots.
 */
public interface ColonyDailySnapshotRepository extends JpaRepository<ColonyDailySnapshot, Long> {

    /**
     * Retrieves one run's snapshots, oldest day first.
     *
     * <p>Serves the population curve, the run's current state as its last element, and — for a closed
     * run — its score and its secondary figures.
     *
     * @param runId run to read
     * @return that run's snapshots ordered by day
     */
    List<ColonyDailySnapshot> findAllByRunIdOrderByDayAsc(Long runId);

    /**
     * Retrieves the last day one run has a snapshot for.
     *
     * <p>Reads the day alone rather than the row, because the only question asked of it is whether a
     * closed run ever reached its settlement day.
     *
     * @param runId run to read
     * @return that run's latest day, or empty when it has no snapshot at all
     */
    @Query("SELECT MAX(snapshot.day) FROM ColonyDailySnapshot snapshot WHERE snapshot.run.id = :runId")
    Optional<LocalDate> findLastDayByRunId(@Param("runId") Long runId);

    /**
     * Deletes one run's snapshots, so the replay can write them again.
     *
     * @param runId run being replayed
     */
    void deleteAllByRunId(Long runId);
}
