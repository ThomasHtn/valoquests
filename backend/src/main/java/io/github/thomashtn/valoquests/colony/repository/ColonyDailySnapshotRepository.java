package io.github.thomashtn.valoquests.colony.repository;

import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * Deletes one run's snapshots, so the replay can write them again.
     *
     * @param runId run being replayed
     */
    void deleteAllByRunId(Long runId);
}
