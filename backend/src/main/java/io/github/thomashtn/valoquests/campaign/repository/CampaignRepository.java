package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to campaigns.
 */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * Returns the campaign that is not closed, if there is one.
     *
     * <p>At most one can exist: a partial unique index enforces it, so this can never have to pick.
     *
     * @param status status to exclude, always {@link CampaignStatus#CLOSED}
     * @return the live campaign, empty between two campaigns
     */
    Optional<Campaign> findByStatusNot(CampaignStatus status);

    /**
     * Returns the closed campaigns, most recent first.
     *
     * @param status status to match, always {@link CampaignStatus#CLOSED}
     * @return closed campaigns
     */
    List<Campaign> findAllByStatusOrderByNumberDesc(CampaignStatus status);

    /**
     * Returns the highest campaign number ever used.
     *
     * @return the last campaign by number, empty on a database that never had one
     */
    Optional<Campaign> findFirstByOrderByNumberDesc();
}
