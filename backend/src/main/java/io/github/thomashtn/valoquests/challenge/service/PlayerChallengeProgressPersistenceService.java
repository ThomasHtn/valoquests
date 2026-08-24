package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists calculated progress for player weekly challenges.
 *
 * <p>This service does not perform challenge calculations. It creates or
 * updates entities from already calculated results and supports batch
 * persistence to avoid one lookup and one write per challenge.</p>
 */
@Service
@Transactional
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
     * <p>This convenience method remains useful for isolated operations. Full
     * weekly recalculations should use {@link #saveAll(Player, List, List)}.</p>
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
        validateArguments(player, weeklyChallenge, result);

        PlayerChallengeProgress progress = progressRepository
            .findByPlayerIdAndWeeklyChallengeId(
                player.getId(),
                weeklyChallenge.getId()
            )
            .orElseGet(
                () -> createProgress(player, weeklyChallenge)
            );

        applyResult(progress, result, clock.instant());

        return progressRepository.save(progress);
    }

    /**
     * Creates or updates all weekly challenge progress rows for one player.
     *
     * <p>Existing rows are loaded with one query and all modified rows are
     * written with one {@code saveAll} operation. Both input lists must use
     * the same order.</p>
     *
     * @param player           player whose progress was calculated
     * @param weeklyChallenges evaluated weekly challenges
     * @param results          calculated results in matching order
     * @return persisted progress rows in weekly-challenge order
     */
    public List<PlayerChallengeProgress> saveAll(
        Player player,
        List<WeeklyChallenge> weeklyChallenges,
        List<ChallengeProgressResult> results
    ) {
        validateBatchArguments(player, weeklyChallenges, results);

        if (weeklyChallenges.isEmpty()) {
            return List.of();
        }

        List<Long> weeklyChallengeIds = weeklyChallenges.stream()
            .map(WeeklyChallenge::getId)
            .toList();

        Map<Long, PlayerChallengeProgress> existingByChallengeId =
            indexExistingProgress(
                progressRepository
                    .findAllByPlayerIdAndWeeklyChallengeIdIn(
                        player.getId(),
                        weeklyChallengeIds
                    )
            );

        Instant calculationTime = clock.instant();

        List<PlayerChallengeProgress> progressRows =
            java.util.stream.IntStream
                .range(0, weeklyChallenges.size())
                .mapToObj(
                    index -> resolveAndUpdateProgress(
                        player,
                        weeklyChallenges.get(index),
                        results.get(index),
                        existingByChallengeId,
                        calculationTime
                    )
                )
                .toList();

        return progressRepository.saveAll(progressRows);
    }

    /**
     * Resolves one existing row or creates it, then applies the new result.
     *
     * @param player               progress owner
     * @param weeklyChallenge      evaluated weekly challenge
     * @param result               calculated result
     * @param existingByChallengeId existing rows indexed by challenge id
     * @param calculationTime      shared batch timestamp
     * @return updated progress row
     */
    private PlayerChallengeProgress resolveAndUpdateProgress(
        Player player,
        WeeklyChallenge weeklyChallenge,
        ChallengeProgressResult result,
        Map<Long, PlayerChallengeProgress> existingByChallengeId,
        Instant calculationTime
    ) {
        PlayerChallengeProgress progress = existingByChallengeId.get(
            weeklyChallenge.getId()
        );

        if (progress == null) {
            progress = createProgress(player, weeklyChallenge);
        }

        applyResult(progress, result, calculationTime);

        return progress;
    }

    /**
     * Indexes existing rows by weekly challenge identifier.
     *
     * @param existingProgress existing progress rows
     * @return mutable identifier index
     */
    private Map<Long, PlayerChallengeProgress> indexExistingProgress(
        List<PlayerChallengeProgress> existingProgress
    ) {
        Map<Long, PlayerChallengeProgress> indexedProgress = new HashMap<>();

        for (PlayerChallengeProgress progress : existingProgress) {
            Long weeklyChallengeId = progress
                .getWeeklyChallenge()
                .getId();

            PlayerChallengeProgress previous = indexedProgress.put(
                weeklyChallengeId,
                progress
            );

            if (previous != null) {
                throw new IllegalStateException(
                    "Several progress rows exist for weekly challenge "
                        + weeklyChallengeId
                        + "."
                );
            }
        }

        return indexedProgress;
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
     * Applies a calculated result to one persistent progress row.
     *
     * @param progress        progress being updated
     * @param result          calculated result
     * @param calculationTime calculation timestamp
     */
    private void applyResult(
        PlayerChallengeProgress progress,
        ChallengeProgressResult result,
        Instant calculationTime
    ) {
        progress.setCurrentValue(result.currentValue());
        progress.setTargetValue(result.targetValue());
        progress.setCalculatedAt(calculationTime);

        updateCompletion(
            progress,
            result.completed(),
            calculationTime
        );
    }

    /**
     * Latches completion and stamps the moment it was first reached.
     *
     * <p>Completion is never taken back. Only the kill-to-death ratio challenge could regress, and
     * letting it do so meant a player who kept playing after validating could lose the challenge's
     * damage and drop a team-bonus tier for everyone else — the exact opposite of what regularity and
     * squad play are meant to be worth here. The measured value below it keeps moving either way, so
     * the progress bar still tells the truth about the current ratio.
     *
     * @param progress        progress being updated
     * @param completed       completion state produced by this calculation
     * @param calculationTime current calculation timestamp
     */
    private void updateCompletion(
        PlayerChallengeProgress progress,
        boolean completed,
        Instant calculationTime
    ) {
        if (progress.isCompleted()) {
            return;
        }

        if (completed) {
            progress.setCompletedAt(calculationTime);
            progress.setCompleted(true);
        }
    }

    /**
     * Validates one persistence operation.
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
        validatePlayer(player);
        validateWeeklyChallenge(weeklyChallenge);
        Objects.requireNonNull(
            result,
            "Challenge progress result must not be null."
        );
    }

    /**
     * Validates one batch persistence operation.
     *
     * @param player           progress owner
     * @param weeklyChallenges evaluated weekly challenges
     * @param results          calculated results
     */
    private void validateBatchArguments(
        Player player,
        List<WeeklyChallenge> weeklyChallenges,
        List<ChallengeProgressResult> results
    ) {
        validatePlayer(player);
        Objects.requireNonNull(
            weeklyChallenges,
            "Weekly challenges must not be null."
        );
        Objects.requireNonNull(
            results,
            "Challenge progress results must not be null."
        );

        if (weeklyChallenges.size() != results.size()) {
            throw new IllegalArgumentException(
                "Weekly challenges and results must have the same size."
            );
        }

        for (int index = 0; index < weeklyChallenges.size(); index++) {
            validateWeeklyChallenge(weeklyChallenges.get(index));
            Objects.requireNonNull(
                results.get(index),
                "Challenge progress result must not be null."
            );
        }
    }

    /**
     * Validates the player used by a persistence operation.
     *
     * @param player progress owner
     */
    private void validatePlayer(Player player) {
        Objects.requireNonNull(player, "Player must not be null.");

        if (player.getId() == null) {
            throw new IllegalArgumentException(
                "The player must be persisted before saving progress."
            );
        }
    }

    /**
     * Validates one weekly challenge used by a persistence operation.
     *
     * @param weeklyChallenge evaluated weekly challenge
     */
    private void validateWeeklyChallenge(
        WeeklyChallenge weeklyChallenge
    ) {
        Objects.requireNonNull(
            weeklyChallenge,
            "Weekly challenge must not be null."
        );

        if (weeklyChallenge.getId() == null) {
            throw new IllegalArgumentException(
                "The weekly challenge must be persisted before saving "
                    + "progress."
            );
        }
    }
}
