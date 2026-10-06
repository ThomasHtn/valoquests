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
 * Owns the per-player, per-season completion flag and pagination checkpoint of the match history walk.
 *
 * <p>Every method commits on its own, after the pages it reflects, so a crash never leaves a season
 * marked complete with missing pages. Callers must therefore never wrap the walk in a transaction.
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
     * <p>An existing state is returned untouched, keeping its flag and checkpoint.
     *
     * @param player tracked player
     * @param season season about to be walked
     * @return the offset a resumed walk may start from and whether the season was already complete
     */
    @Transactional
    public SeasonWalkStart startSeason(Player player, Season season) {
        return stateRepository
            .findByPlayerIdAndSeasonId(player.getId(), season.getId())
            .map(existing -> new SeasonWalkStart(existing.getNextStartOffset(), existing.isComplete()))
            .orElseGet(() -> {
                create(player, season);
                return new SeasonWalkStart(0, false);
            });
    }

    /**
     * Advances the resumable checkpoint of a season being walked.
     *
     * <p>Call only after the page it reflects is committed. The stored offset never moves backward.
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
            .ifPresent(state -> state.setNextStartOffset(newOffset));
    }

    /**
     * Forgets the checkpoint of a season, so no later run jumps to it.
     *
     * <p>Used when newer matches may have shifted the history, so the offset no longer fits.
     *
     * @param playerId tracked player identifier
     * @param seasonId local season identifier
     */
    @Transactional
    public void discardProgress(Long playerId, Long seasonId) {
        stateRepository
            .findByPlayerIdAndSeasonId(playerId, seasonId)
            .ifPresent(state -> state.setNextStartOffset(0));
    }

    /**
     * Marks a season as walked back to its oldest match.
     *
     * <p>Idempotent: an already complete season keeps its original completion instant.
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
                LOGGER.info(
                    "Season synchronization completed: player={} season={}",
                    playerId,
                    seasonId
                );
            });
    }

    /**
     * Finds a season the player started but never finished walking.
     *
     * <p>Empty means the older season is left alone: never targeted, or already complete.
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
     * @param resumeOffset pagination offset a resumed walk may start from, zero for a fresh season
     * @param complete     whether stopping at the first already-stored match is safe
     */
    public record SeasonWalkStart(int resumeOffset, boolean complete) {
    }

    /**
     * Creates the initial, incomplete state of a season.
     */
    private void create(Player player, Season season) {
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
    }
}
