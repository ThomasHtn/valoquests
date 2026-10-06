package io.github.thomashtn.valoquests.maintenance.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resets the derived data with native statements, mirroring
 * {@code V13__reset_derived_synchronization_data.sql}.
 *
 * <p>Challenge selections, their progress, rankings and campaigns are all derived from stored
 * matches: a finalized week whose matches were deleted would report results nothing in the database
 * can justify, so everything derived goes together and is rebuilt from an empty state.
 */
@Service
public class DefaultCampaignResetService implements CampaignResetService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCampaignResetService.class);

    /**
     * Empties every derived table in one statement.
     *
     * <p>{@code CASCADE} is deliberately omitted and every referencing table listed instead, as in
     * the migration: Postgres then accepts the statement only while the list stays complete, so a
     * table added later cannot be silently emptied by this reset — the reset fails loudly instead,
     * which is exactly when someone must decide whether the new table belongs here.
     */
    private static final String TRUNCATE_DERIVED_DATA = """
        TRUNCATE TABLE
            player_challenge_progress,
            weekly_player_score,
            weekly_challenge,
            campaign_player_day,
            campaign_daily_snapshot,
            campaign_week,
            campaign_player,
            campaign,
            player_season_synchronization,
            synchronization_player_result,
            synchronization,
            player_match,
            valorant_match,
            season
        RESTART IDENTITY
        """;

    /**
     * Clears the incremental synchronization watermark of every player.
     *
     * <p>Left as it is, it would claim a history that no longer exists and make the next
     * synchronization stop at matches it never imported.
     */
    private static final String CLEAR_PLAYER_WATERMARKS = """
        UPDATE player
        SET last_successful_synchronization_at = NULL,
            updated_at = now()
        """;

    /**
     * Entity manager used to run the reset statements.
     */
    private final EntityManager entityManager;

    /**
     * Creates the campaign reset service.
     *
     * @param entityManager entity manager
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = """
            The EntityManager is a Spring-managed shared proxy, not a value this service owns:
            copying or wrapping it would break the thread-bound persistence context it delegates to,
            which is exactly what makes the reset participate in the caller's transaction.
            """
    )
    public DefaultCampaignResetService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void resetCampaign() {
        entityManager.createNativeQuery(TRUNCATE_DERIVED_DATA).executeUpdate();
        entityManager.createNativeQuery(CLEAR_PLAYER_WATERMARKS).executeUpdate();

        // Native statements bypass the persistence context, so entities loaded before them are stale.
        entityManager.clear();

        LOGGER.warn("Campaign reset: every match, challenge, ranking and campaign record was cleared");
    }
}
