package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers whether a player sits on a campaign's frozen roster, and therefore whether deleting them
 * would rewrite history.
 *
 * <p>One question, and it is exact: is this player on a campaign's frozen roster. A roster is
 * copied at opening and never changes, so membership is the whole of what a campaign owes a player
 * — its guardians were sized on their presence, and its base was fed by their matches.
 *
 * <p>Deliberately not "did they play a match during a campaign". A roster member who never played a
 * single game still counted: they were a denominator. Archiving is reversible; deleting a player a
 * settled week was sized on is not.
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
