package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Persists calculated progress for player weekly challenges.
 *
 * <p>This service does not perform challenge calculations. It only creates or
 * updates the entity corresponding to an already calculated result.</p>
 */
@Service
public class PlayerChallengeProgressPersistenceService {

    /**
     * Repository used to load and save player progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Application clock used for deterministic timestamps.
     */
    private final Clock clock;

    /**
     * Creates the player challenge progress persistence service.
     *
     * @param progressRepository progress repository
     * @param clock              application clock
     */
    public PlayerChallengeProgressPersistenceService(
        PlayerChallengeProgressRepository progressRepository,
        Clock clock
    ) {
        this.progressRepository = progressRepository;
        this.clock = clock;
    }

    /**
     * Creates or updates one player's progress for one weekly challenge.
     *
     * @param player          player whose progress was calculated
     * @param weeklyChallenge evaluated weekly challenge
     * @param result          calculated progress result
     * @return persisted progress entity
     */
    public PlayerChallengeProgress save(
        Player player,
        WeeklyChallenge weeklyChallenge,
        ChallengeProgressResult result
    ) {
        validateArguments(
            player,
            weeklyChallenge,
            result
        );

        PlayerChallengeProgress progress = progressRepository
            .findByPlayerIdAndWeeklyChallengeId(
                player.getId(),
                weeklyChallenge.getId()
            )
            .orElseGet(
                () -> createProgress(
                    player,
                    weeklyChallenge
                )
            );

        Instant calculationTime = clock.instant();

        progress.setCurrentValue(result.currentValue());
        progress.setTargetValue(result.targetValue());
        progress.setCalculatedAt(calculationTime);

        updateCompletion(
            progress,
            result.completed(),
            calculationTime
        );

        return progressRepository.save(progress);
    }

    /**
     * Creates a new progress entity for a player and weekly challenge.
     *
     * @param player          progress owner
     * @param weeklyChallenge evaluated weekly challenge
     * @return new unsaved progress entity
     */
    private PlayerChallengeProgress createProgress(
        Player player,
        WeeklyChallenge weeklyChallenge
    ) {
        PlayerChallengeProgress progress =
            new PlayerChallengeProgress();

        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);

        return progress;
    }

    /**
     * Updates completion state and its associated timestamp.
     *
     * <p>The first completion timestamp is preserved during later successful
     * recalculations.</p>
     *
     * @param progress        progress being updated
     * @param completed       new completion state
     * @param calculationTime current calculation timestamp
     */
    private void updateCompletion(
        PlayerChallengeProgress progress,
        boolean completed,
        Instant calculationTime
    ) {
        if (completed && progress.getCompletedAt() == null) {
            progress.setCompletedAt(calculationTime);
        }

        if (!completed) {
            progress.setCompletedAt(null);
        }

        progress.setCompleted(completed);
    }

    /**
     * Validates the entities and calculated result before persistence.
     *
     * @param player          progress owner
     * @param weeklyChallenge evaluated weekly challenge
     * @param result          calculated progress result
     */
    private void validateArguments(
        Player player,
        WeeklyChallenge weeklyChallenge,
        ChallengeProgressResult result
    ) {
        Objects.requireNonNull(
            player,
            "Player must not be null."
        );
        Objects.requireNonNull(
            weeklyChallenge,
            "Weekly challenge must not be null."
        );
        Objects.requireNonNull(
            result,
            "Challenge progress result must not be null."
        );

        if (player.getId() == null) {
            throw new IllegalArgumentException(
                "The player must be persisted before saving progress."
            );
        }

        if (weeklyChallenge.getId() == null) {
            throw new IllegalArgumentException(
                "The weekly challenge must be persisted before saving progress."
            );
        }
    }
}
