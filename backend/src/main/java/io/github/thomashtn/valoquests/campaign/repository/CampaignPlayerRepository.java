package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Provides access to the frozen rosters of campaigns.
 */
public interface CampaignPlayerRepository extends JpaRepository<CampaignPlayer, Long> {

    /**
     * Returns one campaign's roster, in a stable order.
     *
     * @param campaignId campaign identifier
     * @return the roster, lowest player identifier first
     */
    List<CampaignPlayer> findAllByCampaignIdOrderByPlayerIdAsc(Long campaignId);

    /**
     * Determines whether a player belongs to any campaign's roster.
     *
     * @param playerId internal player identifier
     * @return {@code true} when a campaign froze the player into its roster
     */
    boolean existsByPlayerId(Long playerId);

    /**
     * Determines whether one campaign froze a player into its roster.
     *
     * @param campaignId campaign identifier
     * @param playerId   internal player identifier
     * @return {@code true} when the player is on that campaign's roster
     */
    boolean existsByCampaignIdAndPlayerId(Long campaignId, Long playerId);

    /**
     * Returns the players any campaign froze into its roster.
     *
     * @return identifiers of every rostered player
     */
    @Query("SELECT DISTINCT member.player.id FROM CampaignPlayer member")
    Set<Long> findAllRosteredPlayerIds();
}
