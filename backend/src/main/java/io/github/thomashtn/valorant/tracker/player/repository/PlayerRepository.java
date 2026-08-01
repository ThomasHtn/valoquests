package io.github.thomashtn.valorant.tracker.player.repository;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
     * Counts tracked players having the requested lifecycle status.
     *
     * @param status player lifecycle status
     * @return number of matching players
     */
    long countByStatus(PlayerStatus status);

    /**
     * Returns tracked players having the requested status in deterministic
     * identifier order.
     *
     * @param status player lifecycle status
     * @return matching players ordered by identifier
     */
    List<Player> findAllByStatusOrderByIdAsc(PlayerStatus status);

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
}
