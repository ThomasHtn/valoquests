package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Draws the challenges a weekly pack is missing, out of the weekly pool.
 *
 * <p>Pure: no repository, no clock. Challenges left in their tier's no-repeat cycle are tried first,
 * the whole tier only as a fallback: no-repeat is a preference, never a reason to hand out an
 * incomplete pack.</p>
 */
final class WeeklyPackDraw {

    /**
     * Not instantiable: static helpers only.
     */
    private WeeklyPackDraw() {
    }

    /**
     * Draws one challenge for every tier the week's existing selections leave empty.
     *
     * @param weekStart          Monday identifying the week being drawn
     * @param drawSalt           salt mixed into the candidate order
     * @param pool               enabled weekly challenges
     * @param existingSelections selections the week already owns
     * @param pastSelections     weekly selections strictly before the week, oldest first
     * @return the missing challenges, empty when no compatible pack can be completed
     */
    static Optional<List<Challenge>> missingChallenges(
        LocalDate weekStart,
        long drawSalt,
        List<Challenge> pool,
        List<ChallengeSelection> existingSelections,
        List<ChallengeSelection> pastSelections
    ) {
        WeeklyPackSelectionState initialState = WeeklyPackSelectionState.from(existingSelections);
        List<ChallengeTier> missingTiers = WeeklyPackSolver.missingTiers(initialState);
        Map<ChallengeTier, List<Challenge>> candidatesByTier = candidatesByTier(pool, weekStart, drawSalt);

        return WeeklyPackSolver.solve(
            WeeklyChallengeCycle.withoutCurrentCycle(candidatesByTier, pastSelections),
            missingTiers,
            initialState
        )
            .or(() -> WeeklyPackSolver.solve(candidatesByTier, missingTiers, initialState));
    }

    /**
     * Groups the pool by tier, each group in a deterministic week-dependent order.
     *
     * <p>The same week therefore produces the same candidate order across application restarts.</p>
     *
     * @param pool      enabled weekly challenges
     * @param weekStart week being drawn
     * @param drawSalt  salt mixed into the candidate order
     * @return candidates grouped by tier
     */
    private static Map<ChallengeTier, List<Challenge>> candidatesByTier(
        List<Challenge> pool,
        LocalDate weekStart,
        long drawSalt
    ) {
        Map<ChallengeTier, List<Challenge>> candidatesByTier = new EnumMap<>(ChallengeTier.class);

        for (ChallengeTier tier : ChallengeTier.values()) {
            candidatesByTier.put(tier, new ArrayList<>());
        }

        pool.stream()
            .sorted(Comparator.comparingLong(challenge -> ChallengeDrawOrder.of(weekStart, challenge, drawSalt)))
            .forEach(challenge -> candidatesByTier.get(challenge.getTier()).add(challenge));

        return candidatesByTier;
    }
}
