package io.github.thomashtn.valoquests.player.repository;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides persistence operations for tracked Valorant players.
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Determines whether a player already owns the given Riot PUUID.
     *
     * @param riotPuuid stable Riot account identifier
     * @return {@code true} when the PUUID is already stored
     */
    boolean existsByRiotPuuid(String riotPuuid);

    /**
     * Determines whether a Riot identity is already tracked, ignoring case.
     *
     * <p>Case-insensitive because Riot itself treats {@code Name#TAG} and {@code name#tag} as the
     * same account: comparing them exactly would let the very same player be added twice.
     *
     * @param gameName Riot game name, excluding the tag line
     * @param tagLine  Riot tag line, excluding the leading hash character
     * @return {@code true} when a player already holds that Riot identity
     */
    boolean existsByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);

    /**
     * Returns tracked players having the requested status in deterministic
     * identifier order.
     *
     * @param status player lifecycle status
     * @return matching players ordered by identifier
     */
    List<Player> findAllByStatusOrderByIdAsc(PlayerStatus status);

    /**
     * Returns every tracked player, regardless of status, in deterministic identifier order.
     *
     * <p>Reserved for administration, which is the only place an archived player must still be
     * visible. Every other caller wants {@link #findAllByStatusNotOrderByIdAsc(PlayerStatus)}.
     *
     * @return every player ordered by identifier
     */
    List<Player> findAllByOrderByIdAsc();

    /**
     * Returns every tracked player except those holding the excluded status, in deterministic
     * identifier order.
     *
     * <p>Used wherever every player must be processed regardless of competition eligibility (e.g.
     * synchronization, challenge progress calculation): an inactive player is still tracked and
     * still completes challenges individually, it just never competes. Callers pass
     * {@link PlayerStatus#ARCHIVED}, which is the one status that means the opposite — the player
     * left the roster and only survives so the history that names it stays readable.
     *
     * @param status status to leave out
     * @return matching players ordered by identifier
     */
    List<Player> findAllByStatusNotOrderByIdAsc(PlayerStatus status);

    /**
     * Returns the most recent successful synchronization instant across every tracked player.
     *
     * <p>Aggregated by the database rather than by loading the players, since the caller only ever
     * needs the maximum.
     *
     * @return the latest instant, or empty when no player was ever synchronized
     */
    @Query("select max(player.lastSuccessfulSynchronizationAt) from Player player")
    Optional<Instant> findLatestSuccessfulSynchronizationAt();

    /**
     * Stores the Riot PUUID resolved for a player, unless its Riot identity changed meanwhile.
     *
     * <p>A targeted update rather than a save of the loaded entity: the Henrik call in between may
     * take long enough for an administrator to edit the player, and a full save would undo that edit.
     *
     * @param playerId  tracked player identifier
     * @param gameName  Riot game name the PUUID was resolved from
     * @param tagLine   Riot tag line the PUUID was resolved from
     * @param riotPuuid resolved Riot PUUID
     * @param updatedAt instant of the update
     * @return number of updated rows, zero when the identity changed meanwhile
     */
    @Transactional
    @Modifying
    @Query("""
        update Player player
        set player.riotPuuid = :riotPuuid,
            player.updatedAt = :updatedAt
        where player.id = :playerId
          and player.gameName = :gameName
          and player.tagLine = :tagLine
        """)
    int storeResolvedPuuid(
        @Param("playerId") Long playerId,
        @Param("gameName") String gameName,
        @Param("tagLine") String tagLine,
        @Param("riotPuuid") String riotPuuid,
        @Param("updatedAt") Instant updatedAt
    );

    /**
     * Stores what a successful synchronization learned about a player, and nothing else.
     *
     * <p>A targeted update rather than a save of the entity loaded minutes earlier, which would
     * overwrite any name, portrait, status or Riot identity an administrator changed meanwhile.
     *
     * @param playerId        tracked player identifier
     * @param competitiveTier current competitive tier
     * @param rankRating      current Rank Rating, {@code null} when unranked
     * @param synchronizedAt  instant the synchronization completed
     */
    @Transactional
    @Modifying
    @Query("""
        update Player player
        set player.competitiveTier = :competitiveTier,
            player.rankRating = :rankRating,
            player.lastSuccessfulSynchronizationAt = :synchronizedAt,
            player.updatedAt = :synchronizedAt
        where player.id = :playerId
        """)
    void recordSuccessfulSynchronization(
        @Param("playerId") Long playerId,
        @Param("competitiveTier") CompetitiveTier competitiveTier,
        @Param("rankRating") Integer rankRating,
        @Param("synchronizedAt") Instant synchronizedAt
    );
}
