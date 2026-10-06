package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.model.CalculatedProgress;
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
 * Persists calculated progress for the challenge selections of one player.
 *
 * <p>Calculates nothing; it batches reads and writes to avoid one round trip per challenge.
 */
@Service
@Transactional
public class PlayerChallengeProgressWriter {

    /**
     * Repository used to load and save player progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Application clock used for deterministic timestamps.
     */
    private final Clock clock;

    /**
     * Creates the player challenge progress writer.
     *
     * @param progressRepository progress repository
     * @param clock              application clock
     */
    public PlayerChallengeProgressWriter(
        PlayerChallengeProgressRepository progressRepository,
        Clock clock
    ) {
        this.progressRepository = progressRepository;
        this.clock = clock;
    }

    /**
     * Creates or updates all challenge selection progress rows for one player.
     *
     * <p>One query loads the existing rows and one {@code saveAll} writes them.
     *
     * @param player     player whose progress was calculated
     * @param calculated each evaluated selection with its calculated result
     * @return persisted progress rows in the order given
     */
    public List<PlayerChallengeProgress> saveAll(Player player, List<CalculatedProgress> calculated) {
        validateBatchArguments(player, calculated);

        if (calculated.isEmpty()) {
            return List.of();
        }

        List<Long> selectionIds = calculated.stream()
            .map(progress -> progress.selection().getId())
            .toList();

        Map<Long, PlayerChallengeProgress> existingBySelectionId =
            indexExistingProgress(
                progressRepository
                    .findAllByPlayerIdAndSelectionIdIn(
                        player.getId(),
                        selectionIds
                    )
            );

        Instant calculationTime = clock.instant();

        List<PlayerChallengeProgress> progressRows = calculated.stream()
            .map(progress -> resolveAndUpdateProgress(
                player,
                progress,
                existingBySelectionId,
                calculationTime
            ))
            .toList();

        return progressRepository.saveAll(progressRows);
    }

    /**
     * Resolves one existing row or creates it, then applies the new result.
     *
     * @param player                progress owner
     * @param calculated            evaluated selection with its calculated result
     * @param existingBySelectionId existing rows indexed by selection id
     * @param calculationTime       shared batch timestamp
     * @return updated progress row
     */
    private PlayerChallengeProgress resolveAndUpdateProgress(
        Player player,
        CalculatedProgress calculated,
        Map<Long, PlayerChallengeProgress> existingBySelectionId,
        Instant calculationTime
    ) {
        PlayerChallengeProgress progress = existingBySelectionId.get(
            calculated.selection().getId()
        );

        if (progress == null) {
            progress = createProgress(player, calculated.selection());
        }

        applyResult(progress, calculated, calculationTime);

        return progress;
    }

    /**
     * Indexes existing rows by challenge selection identifier.
     *
     * @param existingProgress existing progress rows
     * @return mutable identifier index
     */
    private Map<Long, PlayerChallengeProgress> indexExistingProgress(
        List<PlayerChallengeProgress> existingProgress
    ) {
        Map<Long, PlayerChallengeProgress> indexedProgress = new HashMap<>();

        for (PlayerChallengeProgress progress : existingProgress) {
            Long selectionId = progress
                .getSelection()
                .getId();

            PlayerChallengeProgress previous = indexedProgress.put(
                selectionId,
                progress
            );

            if (previous != null) {
                throw new IllegalStateException(
                    "Several progress rows exist for challenge selection "
                        + selectionId
                        + "."
                );
            }
        }

        return indexedProgress;
    }

    /**
     * Creates a new progress entity for a player and a selection.
     *
     * @param player          progress owner
     * @param selection       evaluated selection
     * @return new unsaved progress entity
     */
    private PlayerChallengeProgress createProgress(
        Player player,
        ChallengeSelection selection
    ) {
        PlayerChallengeProgress progress =
            new PlayerChallengeProgress();

        progress.setPlayer(player);
        progress.setSelection(selection);

        return progress;
    }

    /**
     * Applies a calculated result to one persistent progress row.
     *
     * @param progress        progress being updated
     * @param calculated      calculated result
     * @param calculationTime calculation timestamp
     */
    private void applyResult(
        PlayerChallengeProgress progress,
        CalculatedProgress calculated,
        Instant calculationTime
    ) {
        progress.setCurrentValue(calculated.result().currentValue());
        progress.setTargetValue(calculated.result().targetValue());
        progress.setCalculatedAt(calculationTime);

        updateCompletion(
            progress,
            calculated.result().completed(),
            calculationTime
        );
    }

    /**
     * Latches completion and stamps the moment it was first reached.
     *
     * <p>Never taken back, so a falling ratio cannot cost a validated challenge; the measured value still
     * moves.
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
     * Validates one batch persistence operation.
     *
     * @param player     progress owner
     * @param calculated evaluated selections with their results
     */
    private void validateBatchArguments(Player player, List<CalculatedProgress> calculated) {
        validatePlayer(player);
        Objects.requireNonNull(calculated, "Calculated progress must not be null.");

        for (CalculatedProgress progress : calculated) {
            Objects.requireNonNull(progress, "Calculated progress must not be null.");
            validateSelection(progress.selection());
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
     * Validates one selection used by a persistence operation.
     *
     * @param selection evaluated selection
     */
    private void validateSelection(
        ChallengeSelection selection
    ) {
        if (selection.getId() == null) {
            throw new IllegalArgumentException(
                "The challenge selection must be persisted before saving "
                    + "progress."
            );
        }
    }
}
