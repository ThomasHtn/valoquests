package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Locks one campaign's row until the surrounding transaction ends.
     *
     * <p>Serializes replays: each deletes and rewrites the whole campaign, so two running side by side
     * collide on the snapshot's unique day.
     *
     * @param id campaign identifier
     * @return the locked campaign, empty when none owns the identifier
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select campaign from Campaign campaign where campaign.id = :id")
    Optional<Campaign> lockById(@Param("id") Long id);
}
