package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.synchronization.entity.PlayerSeasonSynchronization;
import io.github.thomashtn.valoquests.synchronization.repository.PlayerSeasonSynchronizationRepository;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the per-player, per-season completion flag and pagination checkpoint driving match-history
 * pagination.
 *
 * <p><strong>Every method commits on its own.</strong> That is what makes the flag trustworthy:
 * {@link #startSeason} commits {@code complete = false} before a single match of that season is
 * imported, {@link #recordProgress} commits the checkpoint only after the page it reflects was itself
 * committed, and {@link #markSeasonComplete} commits only after the page that proved the boundary was
 * itself committed. A crash at any point therefore leaves {@code complete = false} with a partial
 * history and a checkpoint no more advanced than what is actually stored, which the next run repairs
 * by resuming from that checkpoint; {@code complete = true} with missing pages is unreachable.
 *
 * <p><strong>Callers must not wrap the walk in a transaction.</strong> Adding {@code @Transactional}
 * above {@link SeasonMatchHistoryWalker#walk} or its callers would join all of these calls into one
 * transaction, defer every commit to the end, and let a rollback erase the {@code complete = false}
 * row along with the imported matches, silently abandoning a season that was being caught up.
 * {@link io.github.thomashtn.valoquests.synchronization.service.DefaultSynchronizationCommandService}
 * is non-transactional for that same reason. Enforced by {@link
 * io.github.thomashtn.valoquests.shared.util.NonTransactionalGuard} at the entry of {@link
 * PlayerSynchronizationService#synchronize}.
 */
@Service
public class SeasonSynchronizationStateService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SeasonSynchronizationStateService.class);

    /**
     * Repository used to load and persist season synchronization state.
     */
    private final PlayerSeasonSynchronizationRepository stateRepository;

    /**
     * Clock used to produce deterministic completion timestamps.
     */
    private final Clock clock;

    /**
     * Creates the season synchronization state service.
     *
     * @param stateRepository repository holding per-player season completion rows
     * @param clock           clock producing deterministic completion timestamps
     */
    public SeasonSynchronizationStateService(
        PlayerSeasonSynchronizationRepository stateRepository,
        Clock clock
    ) {
        this.stateRepository = stateRepository;
        this.clock = clock;
    }

    /**
     * Declares that a season is being walked for a player, creating its state when absent.
     *
     * <p>An existing state is returned untouched, so a season already marked complete keeps its
     * flag and a re-walk of an interrupted season keeps its {@code false} and its checkpoint.
     *
     * @param player tracked player
     * @param season season about to be walked
     * @return the local season identifier and the offset a resumed walk may start from
     */
    @Transactional
    public SeasonWalkStart startSeason(Player player, Season season) {
        return stateRepository
            .findByPlayerIdAndSeasonId(player.getId(), season.getId())
            .map(existing -> new SeasonWalkStart(
                existing.getSeason().getId(),
                existing.getNextStartOffset()
            ))
            .orElseGet(() -> new SeasonWalkStart(create(player, season), 0));
    }

    /**
     * Advances the resumable checkpoint of a season being walked.
     *
     * <p>Called only once the page it reflects has itself been durably imported, so a crash right
     * after this call still leaves a checkpoint no more advanced than what is actually stored.
     * Idempotent and safe under concurrent execution: the stored offset never moves backward.
     *
     * @param playerId  tracked player identifier
     * @param seasonId  local season identifier
     * @param newOffset pagination offset the next resumed walk may start from
     */
    @Transactional
    public void recordProgress(Long playerId, Long seasonId, int newOffset) {
        stateRepository
            .findByPlayerIdAndSeasonId(playerId, seasonId)
            .filter(state -> newOffset > state.getNextStartOffset())
            .ifPresent(state -> {
                state.setNextStartOffset(newOffset);
                stateRepository.save(state);
            });
    }

    /**
     * Marks a season as walked back to its oldest match.
     *
     * <p>Idempotent: re-marking an already complete season preserves the original completion
     * instant, so the timestamp keeps reporting when the history was actually secured.
     *
     * @param playerId tracked player identifier
     * @param seasonId local season identifier
     */
    @Transactional
    public void markSeasonComplete(Long playerId, Long seasonId) {
        stateRepository
            .findByPlayerIdAndSeasonId(playerId, seasonId)
            .filter(state -> !state.isComplete())
            .ifPresent(state -> {
                state.setComplete(true);
                state.setCompletedAt(clock.instant());
                stateRepository.save(state);
                LOGGER.info(
                    "Season synchronization completed: player={} season={}",
                    playerId,
                    seasonId
                );
            });
    }

    /**
     * Indicates whether a season was already walked back to its oldest match.
     *
     * @param playerId tracked player identifier
     * @param seasonId local season identifier
     * @return {@code true} when stopping at the first already-stored match is safe
     */
    @Transactional(readOnly = true)
    public boolean isComplete(Long playerId, Long seasonId) {
        return stateRepository
            .findByPlayerIdAndSeasonId(playerId, seasonId)
            .map(PlayerSeasonSynchronization::isComplete)
            .orElse(false);
    }

    /**
     * Finds a season the player started but never finished walking.
     *
     * <p>Called when the walk crosses into an older season. An empty result means that season must
     * be left alone: either it was never targeted, in which case it is outside the current scope, or
     * it is already complete. A present result is a one-time catch-up, which is how an interrupted
     * run or a season change is repaired without ever widening the scope beyond seasons this
     * application already committed to.
     *
     * @param playerId tracked player identifier
     * @param seasonExternalId Henrik identifier of the season just crossed into
     * @return the local season identifier when the walk must continue into it
     */
    @Transactional(readOnly = true)
    public Optional<Long> findResumableSeasonId(Long playerId, String seasonExternalId) {
        return stateRepository
            .findByPlayerIdAndSeasonExternalId(playerId, seasonExternalId)
            .filter(state -> !state.isComplete())
            .map(state -> state.getSeason().getId());
    }

    /**
     * State of a season a walk is about to start or resume.
     *
     * @param seasonId       local season identifier
     * @param resumeOffset   pagination offset a resumed walk may start from, zero for a fresh season
     */
    public record SeasonWalkStart(Long seasonId, int resumeOffset) {
    }

    /**
     * Creates the initial, incomplete state of a season.
     */
    private Long create(Player player, Season season) {
        PlayerSeasonSynchronization state = new PlayerSeasonSynchronization();
        state.setPlayer(player);
        state.setSeason(season);
        state.setComplete(false);

        stateRepository.save(state);
        LOGGER.info(
            "Season synchronization started: player={} season={} externalId={}",
            player.getId(),
            season.getId(),
            season.getExternalId()
        );
        return season.getId();
    }
}
