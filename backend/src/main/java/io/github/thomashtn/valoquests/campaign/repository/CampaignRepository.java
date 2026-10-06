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
     * Returns the campaign whose status differs from the one given; callers use {@link #findLive()}.
     *
     * @param status status to exclude
     * @return the campaign found
     */
    Optional<Campaign> findByStatusNot(CampaignStatus status);

    /**
     * Returns the campaigns in one status, most recent first; callers use {@link #findAllClosed()}.
     *
     * @param status status to match
     * @return the campaigns found
     */
    List<Campaign> findAllByStatusOrderByNumberDesc(CampaignStatus status);

    /**
     * Returns the most recent campaign in one status; callers use {@link #findLatestClosed()}.
     *
     * @param status status to match
     * @return the campaign found
     */
    Optional<Campaign> findFirstByStatusOrderByNumberDesc(CampaignStatus status);

    /**
     * Returns the campaign that is opened or running, if there is one.
     *
     * <p>At most one can exist: a partial unique index enforces it, so this can never have to pick.
     *
     * @return the live campaign, empty between two campaigns
     */
    default Optional<Campaign> findLive() {
        return findByStatusNot(CampaignStatus.CLOSED);
    }

    /**
     * Returns the closed campaigns, most recent first.
     *
     * @return closed campaigns
     */
    default List<Campaign> findAllClosed() {
        return findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED);
    }

    /**
     * Returns the most recently closed campaign.
     *
     * @return the last closed campaign, empty when none has closed yet
     */
    default Optional<Campaign> findLatestClosed() {
        return findFirstByStatusOrderByNumberDesc(CampaignStatus.CLOSED);
    }

    /**
     * Returns the campaign the site shows: the live one, else the last closed one.
     *
     * @return the campaign shown, empty on a database that never had one
     */
    default Optional<Campaign> findShown() {
        return findLive().or(this::findLatestClosed);
    }

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
