package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * No-repeat cycle of the weekly draw, replayed from past selections.
 *
 * <p>Cycles run per difficulty rather than over the catalogue as a whole: a pack draws exactly one
 * challenge per tier, so tiers empty at their own pace and a shared cycle would let the largest
 * one hold the smallest hostage. A tier holding a single enabled challenge clears on every draw,
 * which is what keeps it drawable at all.</p>
 */
final class WeeklyChallengeCycle {

    /**
     * Not instantiable: static helpers only.
     */
    private WeeklyChallengeCycle() {
    }

    /**
     * Drops the candidates already drawn in the current cycle of their own difficulty.
     *
     * @param candidatesByDifficulty eligible candidates grouped by difficulty
     * @param pastSelections         weekly selections strictly before the week being drawn, oldest first
     * @return the same grouping, keeping only challenges the cycle has not used yet
     */
    static Map<ChallengeDifficulty, List<Challenge>> withoutCurrentCycle(
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty,
        List<WeeklyChallenge> pastSelections
    ) {
        Map<ChallengeDifficulty, Set<Long>> usedByDifficulty =
            usedInCurrentCycle(candidatesByDifficulty, pastSelections);

        Map<ChallengeDifficulty, List<Challenge>> remaining = new EnumMap<>(ChallengeDifficulty.class);

        candidatesByDifficulty.forEach((difficulty, candidates) -> {
            Set<Long> used = usedByDifficulty.getOrDefault(difficulty, Set.of());

            remaining.put(
                difficulty,
                candidates.stream()
                    .filter(candidate -> !used.contains(candidate.getId()))
                    .toList()
            );
        });

        return remaining;
    }

    /**
     * Replays every past selection to determine which challenges were already used in the cycle
     * still in progress, per difficulty, resetting whenever a tier's cycle completes.
     *
     * @param candidatesByDifficulty eligible candidates grouped by difficulty
     * @param pastSelections         weekly selections strictly before the week being drawn, oldest first
     * @return identifiers used since each difficulty's last completed cycle
     */
    private static Map<ChallengeDifficulty, Set<Long>> usedInCurrentCycle(
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty,
        List<WeeklyChallenge> pastSelections
    ) {
        Map<ChallengeDifficulty, Set<Long>> usedByDifficulty = new EnumMap<>(ChallengeDifficulty.class);

        for (WeeklyChallenge selection : pastSelections) {
            Challenge challenge = selection.getChallenge();
            ChallengeDifficulty difficulty = challenge.getDifficulty();

            Set<Long> used = usedByDifficulty.computeIfAbsent(difficulty, tier -> new HashSet<>());
            used.add(challenge.getId());

            int tierSize = candidatesByDifficulty.getOrDefault(difficulty, List.of()).size();

            if (used.size() >= tierSize) {
                used.clear();
            }
        }

        return usedByDifficulty;
    }
}
