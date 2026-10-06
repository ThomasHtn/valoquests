package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers whether a player sits on a campaign's frozen roster, so deleting them would rewrite history.
 *
 * <p>Membership, not matches played: a roster member who never played still sized the guardians.
 */
@Service
public class CampaignRosterMembership {

    /**
     * Repository holding every campaign's frozen roster.
     */
    private final CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Repository locating the campaign opened or running, if any.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Creates the campaign roster membership.
     *
     * @param campaignPlayerRepository campaign roster repository
     * @param campaignRepository       campaign repository
     */
    public CampaignRosterMembership(
        CampaignPlayerRepository campaignPlayerRepository,
        CampaignRepository campaignRepository
    ) {
        this.campaignPlayerRepository = campaignPlayerRepository;
        this.campaignRepository = campaignRepository;
    }

    /**
     * Determines whether any campaign, live or closed, froze a player into its roster.
     *
     * @param playerId tracked player identifier
     * @return {@code true} when a campaign froze the player into its roster
     */
    @Transactional(readOnly = true)
    public boolean wasOnAnyRoster(long playerId) {
        return campaignPlayerRepository.existsByPlayerId(playerId);
    }

    /**
     * Returns every player any campaign froze into its roster.
     *
     * @return identifiers of the players a campaign froze into its roster
     */
    @Transactional(readOnly = true)
    public Set<Long> rosteredPlayerIds() {
        return campaignPlayerRepository.findAllRosteredPlayerIds();
    }

    /**
     * Determines whether a campaign is opened or running, its roster frozen.
     *
     * @return {@code true} while a campaign is live
     */
    @Transactional(readOnly = true)
    public boolean isCampaignLive() {
        return campaignRepository.findLive().isPresent();
    }

    /**
     * Determines whether a player sits on the frozen roster of the live campaign.
     *
     * @param playerId tracked player identifier
     * @return {@code true} when the live campaign counts the player
     */
    @Transactional(readOnly = true)
    public boolean isOnLiveRoster(long playerId) {
        return campaignRepository.findLive()
            .map(campaign -> campaignPlayerRepository.existsByCampaignIdAndPlayerId(campaign.getId(), playerId))
            .orElse(false);
    }
}
