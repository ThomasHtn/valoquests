package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to a campaign's daily snapshots.
 */
public interface CampaignDailySnapshotRepository extends JpaRepository<CampaignDailySnapshot, Long> {

    /**
     * Returns one campaign's days, oldest first.
     *
     * @param campaignId campaign identifier
     * @return every computed day in order
     */
    List<CampaignDailySnapshot> findAllByCampaignIdOrderByDayAsc(Long campaignId);

    /**
     * Returns one day of one campaign.
     *
     * @param campaignId campaign identifier
     * @param day        calendar day
     * @return the day, empty when the replay never reached it
     */
    Optional<CampaignDailySnapshot> findByCampaignIdAndDay(Long campaignId, LocalDate day);

    /**
     * Deletes every day of one campaign, so a replay can write them again.
     *
     * @param campaignId campaign identifier
     */
    void deleteAllByCampaignId(Long campaignId);
}
