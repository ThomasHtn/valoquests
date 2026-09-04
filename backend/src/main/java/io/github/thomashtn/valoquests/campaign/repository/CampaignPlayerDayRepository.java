package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to a campaign's per-operator days.
 */
public interface CampaignPlayerDayRepository extends JpaRepository<CampaignPlayerDay, Long> {

    /**
     * Returns every operator day of one campaign inside a range, oldest first.
     *
     * @param campaignId campaign identifier
     * @param firstDay   first day of the range, inclusive
     * @param lastDay    last day of the range, inclusive
     * @return the operator days in order
     */
    List<CampaignPlayerDay> findAllByCampaignIdAndDayBetweenOrderByDayAsc(
        Long campaignId,
        LocalDate firstDay,
        LocalDate lastDay
    );

    /**
     * Returns every day of one operator in one campaign, oldest first.
     *
     * @param campaignId campaign identifier
     * @param playerId   internal player identifier
     * @return the operator's days in order
     */
    List<CampaignPlayerDay> findAllByCampaignIdAndPlayerIdOrderByDayAsc(Long campaignId, Long playerId);

    /**
     * Deletes every operator day of one campaign, so a replay can write them again.
     *
     * @param campaignId campaign identifier
     */
    void deleteAllByCampaignId(Long campaignId);
}
