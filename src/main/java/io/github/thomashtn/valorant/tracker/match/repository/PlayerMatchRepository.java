package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Provides persistence operations for player match entities.
 */
public interface PlayerMatchRepository
    extends JpaRepository<PlayerMatch, Long> {

    /**
     * Determines whether a player-match association already exists.
     *
     * @param playerId internal player identifier
     * @param matchId  internal match identifier
     * @return {@code true} when the association already exists
     */
    boolean existsByPlayerIdAndMatchId(Long playerId, Long matchId);

    /**
     * Retrieves the matches played by a player during a half-open period.
     *
     * <p>The beginning is inclusive and the end is exclusive. The associated
     * Valorant match is loaded in the same query to prevent additional lazy
     * loading queries during challenge calculations.</p>
     *
     * @param playerId    internal player identifier
     * @param periodStart inclusive beginning of the period
     * @param periodEnd   exclusive end of the period
     * @return player matches ordered chronologically
     */
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            JOIN FETCH playerMatch.match valorantMatch
            WHERE playerMatch.player.id = :playerId
              AND valorantMatch.startedAt >= :periodStart
              AND valorantMatch.startedAt < :periodEnd
            ORDER BY valorantMatch.startedAt ASC,
                     playerMatch.id ASC
            """
    )
    List<PlayerMatch> findForChallengePeriod(
        @Param("playerId") Long playerId,
        @Param("periodStart") Instant periodStart,
        @Param("periodEnd") Instant periodEnd
    );
}
