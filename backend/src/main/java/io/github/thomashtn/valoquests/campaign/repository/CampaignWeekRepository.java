package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import java.util.List;
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
}
