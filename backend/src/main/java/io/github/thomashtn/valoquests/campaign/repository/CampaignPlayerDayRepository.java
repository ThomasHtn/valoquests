package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to a campaign's per-player days.
 */
public interface CampaignPlayerDayRepository extends JpaRepository<CampaignPlayerDay, Long> {

    /**
     * Returns every player day of one campaign on one day.
     *
     * @param campaignId campaign identifier
     * @param day        calendar day
     * @return the player days, in no particular order
     */
    List<CampaignPlayerDay> findAllByCampaignIdAndDay(Long campaignId, LocalDate day);

    /**
     * Deletes every player day of one campaign, so a replay can write them again.
     *
     * @param campaignId campaign identifier
     */
    void deleteAllByCampaignId(Long campaignId);
}
