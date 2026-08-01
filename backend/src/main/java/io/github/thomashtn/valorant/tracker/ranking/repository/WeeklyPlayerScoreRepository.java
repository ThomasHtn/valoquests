package io.github.thomashtn.valorant.tracker.ranking.repository;

import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Provides persistence operations for weekly player score entities.
 */
public interface WeeklyPlayerScoreRepository
    extends JpaRepository<WeeklyPlayerScore, Long> {

    /**
     * Retrieves every score for one week with its player in ranking order.
     *
     * @param weekStart Monday identifying the week
     * @return weekly scores ordered by position
     */
    @EntityGraph(attributePaths = "player")
    List<WeeklyPlayerScore> findAllByWeekStartOrderByPositionAsc(
        LocalDate weekStart
    );

    /**
     * Retrieves every score for the requested weeks with players preloaded.
     *
     * @param weekStarts week identifiers
     * @return matching scores ordered by week and position
     */
    @EntityGraph(attributePaths = "player")
    List<WeeklyPlayerScore> findAllByWeekStartInOrderByWeekStartDescPositionAsc(
        Collection<LocalDate> weekStarts
    );

    /**
     * Returns finalized week identifiers using week-based pagination.
     *
     * @param pageable pagination parameters
     * @return page of distinct finalized weeks
     */
    @Query(
        value = """
            select distinct score.weekStart
            from WeeklyPlayerScore score
            where score.finalizedAt is not null
            order by score.weekStart desc
            """,
        countQuery = """
            select count(distinct score.weekStart)
            from WeeklyPlayerScore score
            where score.finalizedAt is not null
            """
    )
    Page<LocalDate> findFinalizedWeekStarts(Pageable pageable);

    /**
     * Deletes scores belonging to players that are no longer active.
     *
     * @param weekStart current week identifier
     * @param activePlayerIds active tracked player identifiers
     */
    void deleteAllByWeekStartAndPlayerIdNotIn(
        LocalDate weekStart,
        Collection<Long> activePlayerIds
    );

    /**
     * Deletes every score for a week when there are no active players.
     *
     * @param weekStart current week identifier
     */
    void deleteAllByWeekStart(LocalDate weekStart);
}
