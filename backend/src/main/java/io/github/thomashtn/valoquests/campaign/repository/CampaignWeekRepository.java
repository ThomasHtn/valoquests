package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to the ten weeks of a campaign.
 */
public interface CampaignWeekRepository extends JpaRepository<CampaignWeek, Long> {

    /**
     * Returns one campaign's weeks, week one first.
     *
     * @param campaignId campaign identifier
     * @return the ten weeks in order
     */
    List<CampaignWeek> findAllByCampaignIdOrderByWeekIndexAsc(Long campaignId);

    /**
     * Returns the week a Monday belongs to inside one campaign.
     *
     * @param campaignId campaign identifier
     * @param weekStart  Monday identifying the week
     * @return the week, empty when the Monday falls outside the campaign
     */
    Optional<CampaignWeek> findByCampaignIdAndWeekStart(Long campaignId, LocalDate weekStart);
}
