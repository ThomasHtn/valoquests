package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /** Returns a filtered page of matches for one tracked player. */
    @EntityGraph(attributePaths = {"player", "match", "match.season"})
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            JOIN playerMatch.match valorantMatch
            WHERE playerMatch.player.id = :playerId
              AND (:seasonId IS NULL OR valorantMatch.season.id = :seasonId)
              AND (:map IS NULL OR LOWER(valorantMatch.mapName) = LOWER(CAST(:map AS string)))
              AND (:agent IS NULL OR LOWER(playerMatch.agentName) = LOWER(CAST(:agent AS string)))
              AND (:result IS NULL OR playerMatch.result = :result)
            """
    )
    Page<PlayerMatch> findHistory(
        @Param("playerId") Long playerId,
        @Param("seasonId") Long seasonId,
        @Param("map") String map,
        @Param("agent") String agent,
        @Param("result") io.github.thomashtn.valorant.tracker.match.model.MatchResult result,
        Pageable pageable
    );

    /** Returns all matches required to calculate one player's profile statistics. */
    @EntityGraph(attributePaths = {"match", "match.season"})
    List<PlayerMatch> findAllByPlayerIdOrderByMatchStartedAtDesc(Long playerId);
}

