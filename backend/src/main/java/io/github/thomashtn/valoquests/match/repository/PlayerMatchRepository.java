package io.github.thomashtn.valoquests.match.repository;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * Retrieves the matches of every player holding one status over a half-open period, in one query.
     *
     * <p>The beginning is inclusive and the end is exclusive, as in {@link #findForChallengePeriod}.
     * Exists for the colony replay, which prices a whole run — eleven weeks — for the entire roster:
     * asking per player and per week made that one call cost {@code players x weeks} round trips, and
     * the replay runs after every synchronization. Both the player and the Valorant match are fetched
     * in the same query, since the caller reads both on every row.
     *
     * <p>Filtered on the status rather than left open. The colony names its roster through
     * {@link io.github.thomashtn.valoquests.player.entity.Player#COMPETITIVE_STATUS} everywhere else —
     * the size a run freezes, the turnout readout, the ranking — and this query being the one place
     * that did not meant a deactivated player kept feeding the town while appearing on none of its
     * gauges. A day's harvest could then have no author anywhere in the interface.
     *
     * @param status      status a player must hold for their matches to be returned
     * @param periodStart inclusive beginning of the period
     * @param periodEnd   exclusive end of the period
     * @return those players' matches over the period, ordered chronologically
     */
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            JOIN FETCH playerMatch.match valorantMatch
            JOIN FETCH playerMatch.player player
            WHERE player.status = :status
              AND valorantMatch.startedAt >= :periodStart
              AND valorantMatch.startedAt < :periodEnd
            ORDER BY valorantMatch.startedAt ASC,
                     playerMatch.id ASC
            """
    )
    List<PlayerMatch> findAllForPeriod(
        @Param("status") PlayerStatus status,
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
     * Returns one player's matches inside a set of seasons, most recent first.
     *
     * <p>Exists so the progression endpoint's season filter is applied by the database rather than
     * by discarding rows in memory: its analytics span a whole career, so loading every stored
     * match to keep one act's worth grows with the player's history instead of with the answer.
     *
     * <p>Every game mode is returned, not just competitive: the personal-records section measures
     * the active-day streak across all of them.
     *
     * @param playerId  internal player identifier
     * @param seasonIds seasons to keep; must be non-empty, since an empty {@code IN} matches nothing
     * @return the player's matches inside those seasons, most recent first
     */
    @EntityGraph(attributePaths = {"match", "match.season"})
    List<PlayerMatch> findAllByPlayerIdAndMatchSeasonIdInOrderByMatchStartedAtDesc(
        Long playerId,
        Collection<Long> seasonIds
    );

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

    /**
     * Loads one tracked player's statistics for one match, scoped to that player.
     *
     * <p>Scoped by {@code playerId} rather than looked up by {@code id} alone, so a match detail
     * request can never resolve to a row belonging to a different tracked player than the one named
     * in the request path.
     *
     * @param id       internal player-match identifier
     * @param playerId internal player identifier the match must belong to
     * @return the matching player-match, when it exists and belongs to that player
     */
    @EntityGraph(attributePaths = {"player", "match", "match.season"})
    Optional<PlayerMatch> findByIdAndPlayerId(Long id, Long playerId);

    /**
     * Loads every other tracked player's statistics for the same underlying match.
     *
     * <p>The squad is small enough that two tracked players routinely land in the same lobby; a
     * match's detail surfaces every one of them found in the same match, on either team, rather than
     * only the requesting player's own row.
     *
     * @param matchId  internal identifier of the shared underlying match
     * @param playerId internal identifier of the player whose own row is excluded
     * @return the other tracked players' statistics for that match
     */
    @EntityGraph(attributePaths = {"player"})
    List<PlayerMatch> findByMatchIdAndPlayerIdNot(Long matchId, Long playerId);
}

