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
     * Determines whether a player has a match imported after an instant.
     *
     * @param playerId   internal player identifier
     * @param importedAt exclusive lower bound on the import instant
     * @return {@code true} when at least one match was imported after the bound
     */
    boolean existsByPlayerIdAndCreatedAtAfter(Long playerId, Instant importedAt);

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
     * <p>The Valorant match is fetched in the same query, since every caller reads it.
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
     * <p>One query for the whole roster and period, because the campaign replay prices up to eleven
     * weeks after every synchronization.
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
     * <p>Every {@link PlayerMatchHistoryCriteria} field is ignored when {@code null}.
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
     * <p>The roster is the one frozen at the campaign's opening, narrowed to the given statuses.
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
     * <p>Every game mode is returned: the personal records measure the active-day streak across all.
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
     * Returns the squad's matches of one game mode, optionally inside one season.
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
     * <p>Scoped by {@code playerId} so a request never resolves to another player's row.
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
     * @param matchId  internal identifier of the shared underlying match
     * @param playerId internal identifier of the player whose own row is excluded
     * @return the other tracked players' statistics for that match
     */
    @EntityGraph(attributePaths = {"player"})
    List<PlayerMatch> findByMatchIdAndPlayerIdNot(Long matchId, Long playerId);
}
