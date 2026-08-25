package io.github.thomashtn.valoquests.maintenance.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationLaunchService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wipes every piece of data derived from match history, so a new campaign starts from a coherent
 * base.
 *
 * <p>This is the runtime counterpart of {@code V13__reset_derived_synchronization_data.sql}, which
 * documents why these tables go together: challenge selections, their progress and the rankings and
 * boss encounters built on top of them are all derived from stored matches. A finalized week whose
 * matches were deleted would report results nothing in the database can justify, so rebuilding from
 * an empty state is the only option that keeps the campaign traceable to stored matches.
 *
 * <p>Runs and colony snapshots go with them. A run is the ten-week window the campaign is bounded by
 * and a snapshot is a pure function of the matches, challenges and boss outcomes inside it, so both
 * describe history that this reset is deleting. The next rollover opens run 1 on the empty base.
 *
 * <p>Deliberately kept: the player roster, the challenge catalogue and the boss catalogue. None of
 * them is derived from anything.
 */
@Service
public class CampaignResetService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignResetService.class);

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
            colony_daily_snapshot,
            weekly_boss_encounter,
            run,
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
     * Service reporting whether a synchronization is currently running.
     */
    private final SynchronizationLaunchService synchronizationLaunchService;

    /**
     * Creates the campaign reset service.
     *
     * @param entityManager                entity manager
     * @param synchronizationLaunchService synchronization launch service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = """
            The EntityManager is a Spring-managed shared proxy, not a value this service owns:
            copying or wrapping it would break the thread-bound persistence context it delegates to,
            which is exactly what makes the reset participate in the caller's transaction.
            """
    )
    public CampaignResetService(
        EntityManager entityManager,
        SynchronizationLaunchService synchronizationLaunchService
    ) {
        this.entityManager = entityManager;
        this.synchronizationLaunchService = synchronizationLaunchService;
    }

    /**
     * Empties every derived table and rewinds the roster's synchronization state.
     *
     * @throws ConflictException when a synchronization is in progress
     */
    @Transactional
    public void resetCampaign() {
        if (synchronizationLaunchService.isSynchronizationInProgress()) {
            throw new ConflictException(
                "A synchronization is in progress. Wait for it to finish before resetting the "
                    + "campaign, otherwise it would write matches into the base being emptied."
            );
        }

        entityManager.createNativeQuery(TRUNCATE_DERIVED_DATA).executeUpdate();
        entityManager.createNativeQuery(CLEAR_PLAYER_WATERMARKS).executeUpdate();

        // Both statements are native, so they bypass the persistence context entirely. Anything
        // loaded before the reset now describes rows that were deleted or rewritten, and would be
        // handed back unchanged — including the watermark this method just cleared.
        entityManager.clear();

        LOGGER.warn("Campaign reset: every match, challenge, ranking and boss record was cleared");
    }
}
