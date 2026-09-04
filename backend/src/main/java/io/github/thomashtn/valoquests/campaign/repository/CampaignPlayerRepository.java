package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
