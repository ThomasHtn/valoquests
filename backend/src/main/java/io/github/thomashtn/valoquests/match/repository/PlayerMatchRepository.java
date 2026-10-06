package io.github.thomashtn.valoquests.match.repository;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
     * Returns the players with a stored match started at or after an instant.
     *
     * @param startedAt inclusive lower bound
     * @return identifiers of the players who played since the bound
     */
    @Query(
        """
            SELECT DISTINCT playerMatch.player.id
            FROM PlayerMatch playerMatch
            WHERE playerMatch.match.startedAt >= :startedAt
            """
    )
    Set<Long> findPlayerIdsWithMatchStartedSince(@Param("startedAt") Instant startedAt);

    /**
     * Returns when the newest match a player has stored in one season started.
     *
     * @param playerId internal player identifier
     * @param seasonId internal season identifier
     * @return start instant of that match, empty when the player has none stored in the season
     */
    @Query(
        """
            SELECT MAX(valorantMatch.startedAt)
            FROM PlayerMatch playerMatch
            JOIN playerMatch.match valorantMatch
            WHERE playerMatch.player.id = :playerId
              AND valorantMatch.season.id = :seasonId
            """
    )
    Optional<Instant> findNewestMatchStartInSeason(
        @Param("playerId") Long playerId,
        @Param("seasonId") Long seasonId
    );

    /**
     * Retrieves the matches played by a player during a half-open period.
     *
     * <p>The beginning is inclusive and the end is exclusive. The associated Valorant match is
     * loaded in the same query, since every caller reads it on each row.</p>
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
    List<PlayerMatch> findByPlayerInPeriod(
        @Param("playerId") Long playerId,
        @Param("periodStart") Instant periodStart,
        @Param("periodEnd") Instant periodEnd
    );

    /**
     * Retrieves the matches of every player holding one of several statuses over a half-open period.
     *
     * <p>The beginning is inclusive and the end is exclusive, as in {@link #findByPlayerInPeriod}.
     * One query for the whole roster and the whole period, player and match fetched with it, because
     * the campaign replay prices up to eleven weeks after every synchronization.
     *
     * <p>Each caller states the statuses it needs: the campaign replay reads every status and keeps
     * its frozen roster, the rankings read the players they list.
     *
     * @param statuses    statuses a player may hold for their matches to be returned
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
            WHERE player.status IN :statuses
              AND valorantMatch.startedAt >= :periodStart
              AND valorantMatch.startedAt < :periodEnd
            ORDER BY valorantMatch.startedAt ASC,
                     playerMatch.id ASC
            """
    )
    List<PlayerMatch> findByPlayerStatusesInPeriod(
        @Param("statuses") Collection<PlayerStatus> statuses,
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
     * Returns a page of the matches a campaign's roster played over a half-open period.
     *
     * <p>Backs the squad's shared history, so the player is fetched with each row: every entry is
     * named after who played it. The roster is the one frozen at the campaign's opening, further
     * narrowed to the statuses the caller lists.
     *
     * @param campaignId  campaign whose roster the players must belong to
     * @param statuses    statuses a player may hold for their matches to be returned
     * @param periodStart inclusive beginning of the period
     * @param periodEnd   exclusive end of the period
     * @param pageable    pagination and sort parameters
     * @return the requested page of matches
     */
    @EntityGraph(attributePaths = {"player", "match", "match.season"})
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            WHERE playerMatch.player.status IN :statuses
              AND playerMatch.player.id IN (
                SELECT rosterEntry.player.id
                FROM CampaignPlayer rosterEntry
                WHERE rosterEntry.campaign.id = :campaignId
              )
              AND playerMatch.match.startedAt >= :periodStart
              AND playerMatch.match.startedAt < :periodEnd
            """
    )
    Page<PlayerMatch> findSquadHistory(
        @Param("campaignId") Long campaignId,
        @Param("statuses") Collection<PlayerStatus> statuses,
        @Param("periodStart") Instant periodStart,
        @Param("periodEnd") Instant periodEnd,
        Pageable pageable
    );

    /**
     * Returns every stored match of one player.
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
     * Returns the matches of one game mode, optionally inside one season, of every player not
     * holding a given status.
     *
     * <p>Loads the whole squad in one query, for lists that would otherwise query once per player.
     *
     * @param seasonId       season to keep, or {@code null} for every season
     * @param gameMode       game mode to keep
     * @param excludedStatus status whose players' matches are left out
     * @return matching matches, most recent first
     */
    @EntityGraph(attributePaths = {"match", "match.season"})
    @Query(
        """
            SELECT playerMatch
            FROM PlayerMatch playerMatch
            JOIN playerMatch.match valorantMatch
            WHERE (:seasonId IS NULL OR valorantMatch.season.id = :seasonId)
              AND valorantMatch.gameMode = :gameMode
              AND playerMatch.player.status <> :excludedStatus
            ORDER BY valorantMatch.startedAt DESC
            """
    )
    List<PlayerMatch> findAllBySeasonAndGameModeAndPlayerStatusNot(
        @Param("seasonId") Long seasonId,
        @Param("gameMode") GameMode gameMode,
        @Param("excludedStatus") PlayerStatus excludedStatus
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

