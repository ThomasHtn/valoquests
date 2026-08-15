package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Determines whether a player has a stored match started at or after an instant.
     *
     * @param playerId internal player identifier
     * @param startedAt inclusive lower bound
     * @return {@code true} when at least one stored match starts at or after the bound
     */
    boolean existsByPlayerIdAndMatchStartedAtGreaterThanEqual(Long playerId, Instant startedAt);

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

    /**
     * Returns a filtered page of matches for one tracked player.
     *
     * <p>Every {@link PlayerMatchHistoryCriteria} field is optional and ignored when {@code null},
     * so one query serves the unfiltered history and every combination the match page offers.
     * Criteria are bundled into one parameter, rather than passed individually, to keep this method
     * under the project's parameter-count limit.
     *
     * @param playerId internal player identifier
     * @param criteria optional season, map, agent, result, game mode and week-range filters
     * @param pageable pagination and sort parameters
     * @return the requested page of matches
     */
    @EntityGraph(attributePaths = {"player", "match", "match.season"})
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            JOIN playerMatch.match valorantMatch
            WHERE playerMatch.player.id = :playerId
              AND (:#{#criteria.seasonId} IS NULL OR valorantMatch.season.id = :#{#criteria.seasonId})
              AND (:#{#criteria.map} IS NULL
                OR LOWER(valorantMatch.mapName) = LOWER(CAST(:#{#criteria.map} AS string)))
              AND (:#{#criteria.agent} IS NULL
                OR LOWER(playerMatch.agentName) = LOWER(CAST(:#{#criteria.agent} AS string)))
              AND (:#{#criteria.result} IS NULL OR playerMatch.result = :#{#criteria.result})
              AND (:#{#criteria.gameMode} IS NULL OR valorantMatch.gameMode = :#{#criteria.gameMode})
              AND valorantMatch.startedAt >= :#{#criteria.periodStart}
              AND valorantMatch.startedAt < :#{#criteria.periodEnd}
            """
    )
    Page<PlayerMatch> findHistory(
        @Param("playerId") Long playerId,
        @Param("criteria") PlayerMatchHistoryCriteria criteria,
        Pageable pageable
    );

    /**
     * Returns all matches required to calculate one player's profile statistics.
     *
     * @param playerId internal player identifier
     * @return every stored match of the player, most recent first
     */
    @EntityGraph(attributePaths = {"match", "match.season"})
    List<PlayerMatch> findAllByPlayerIdOrderByMatchStartedAtDesc(Long playerId);

    /**
     * Returns the matches used to calculate one player's profile statistics, filtered by season,
     * game mode and a week range.
     *
     * <p>Filters are bundled into {@link PlayerMatchHistoryCriteria} - {@code map}, {@code agent}
     * and {@code result} are simply left {@code null} by callers, since statistics have no use for
     * them - rather than bound individually, mirroring {@link #findHistory}. See that criteria
     * type's Javadoc for why {@code periodStart}/{@code periodEnd} must never be {@code null}.
     *
     * @param playerId internal player identifier
     * @param criteria season, game mode and week-range filters; {@code map}, {@code agent} and
     *     {@code result} are ignored
     * @return matching matches, most recent first
     */
    @EntityGraph(attributePaths = {"match", "match.season"})
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            JOIN playerMatch.match valorantMatch
            WHERE playerMatch.player.id = :playerId
              AND (:#{#criteria.seasonId} IS NULL OR valorantMatch.season.id = :#{#criteria.seasonId})
              AND (:#{#criteria.gameMode} IS NULL OR valorantMatch.gameMode = :#{#criteria.gameMode})
              AND valorantMatch.startedAt >= :#{#criteria.periodStart}
              AND valorantMatch.startedAt < :#{#criteria.periodEnd}
            ORDER BY valorantMatch.startedAt DESC
            """
    )
    List<PlayerMatch> findAllByPlayerIdAndSeasonAndGameMode(
        @Param("playerId") Long playerId,
        @Param("criteria") PlayerMatchHistoryCriteria criteria
    );
}

