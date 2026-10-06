package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.ranking.service.WeekCoverageSource;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Answers the ranking's question of which weeks a campaign covered.
 *
 * <p>A campaign still waiting for its first Monday covers nothing yet.
 */
@Component
public class CampaignWeekCoverageSource implements WeekCoverageSource {

    /**
     * Repository telling which weeks a campaign covered.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Creates the campaign-backed coverage source.
     *
     * @param campaignRepository campaign repository
     */
    public CampaignWeekCoverageSource(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    /**
     * Tells whether a campaign, running or already closed, covered one week.
     *
     * @param weekStart Monday identifying the week
     * @return {@code true} when a campaign covered it
     */
    @Override
    public boolean coveredByCampaign(LocalDate weekStart) {
        return campaignRepository.findAll().stream()
            .filter(campaign -> campaign.getStatus() != CampaignStatus.OPENED)
            .anyMatch(campaign -> campaign.covers(weekStart));
    }
}
