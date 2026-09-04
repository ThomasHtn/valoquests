package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides whether a player took part in a campaign, and therefore whether deleting them would
 * rewrite history.
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
public class PlayerCampaignContributionResolver {

    /**
     * Repository holding every campaign's frozen roster.
     */
    private final CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Creates the campaign contribution resolver.
     *
     * @param campaignPlayerRepository campaign roster repository
     */
    public PlayerCampaignContributionResolver(CampaignPlayerRepository campaignPlayerRepository) {
        this.campaignPlayerRepository = campaignPlayerRepository;
    }

    /**
     * Determines whether a player contributed to a campaign.
     *
     * @param playerId tracked player identifier
     * @return {@code true} when a campaign froze the player into its roster
     */
    @Transactional(readOnly = true)
    public boolean hasContributed(long playerId) {
        return campaignPlayerRepository.existsByPlayerId(playerId);
    }
}
