package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyWeekOutcome;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds up what one finished week hands the colony.
 *
 * <p>Both sources are credited at the weekly rollover, in one go, once the week is finalized and both
 * the completions and the fight's outcome are known. Attributing each challenge to the exact day it was
 * validated would have meant replaying the progress calculators day by day, on a subsystem that has
 * never had that notion — by far the most expensive and most fragile piece of the design, for the sole
 * comfort of seeing materials on a Tuesday rather than the following Monday.
 *
 * <p>The fight is the only thing in the whole model that moves morale, and it is the run's biggest
 * lever by some distance. It pays in materials rather than in inhabitants on purpose: a gift of
 * inhabitants fades at fifteen percent a night and is gone three weeks later, so the first six fights of
 * a run would have counted for nothing.
 */
@Service
@Transactional(readOnly = true)
public class ColonyMaterialsReader {

    /**
     * Repository holding which player completed which challenge of a week.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Repository holding a week's fight and its outcome.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Ruleset pricing a completed challenge, a defeated boss and a surviving one.
     */
    private final ColonyRuleset ruleset;

    /**
     * Creates the materials reader.
     *
     * @param progressRepository  player challenge progress repository
     * @param encounterRepository weekly boss encounter repository
     * @param ruleset             colony ruleset
     */
    public ColonyMaterialsReader(
        PlayerChallengeProgressRepository progressRepository,
        WeeklyBossEncounterRepository encounterRepository,
        ColonyRuleset ruleset
    ) {
        this.progressRepository = progressRepository;
        this.encounterRepository = encounterRepository;
        this.ruleset = ruleset;
    }

    /**
     * Returns what a week hands over, challenges and fight together.
     *
     * <p>A week with no encounter at all is simply a week with no fight: no materials, and no morale
     * either way. That is what keeps a run ten weeks long instead of ten fights long, and therefore what
     * keeps runs comparable.
     *
     * @param weekStart  Monday identifying the finished week
     * @param rosterSize roster size frozen on the run, which the fight's materials are priced per player
     *     of
     * @return the week's materials and morale
     */
    public ColonyWeekOutcome outcomeOf(LocalDate weekStart, int rosterSize) {
        Optional<WeeklyBossEncounter> encounter = encounterRepository.findByWeekStart(weekStart)
            .filter(fight -> fight.getFinalizedAt() != null);

        int materials = challengeMaterials(weekStart) + encounter
            .filter(WeeklyBossEncounter::isDefeated)
            .map(fight -> ruleset.materialsForDefeatedBoss(
                fight.getBossCatalogEntry().getCategory(),
                rosterSize
            ))
            .orElse(0);

        double moraleDelta = encounter
            .map(fight -> fight.isDefeated()
                ? ruleset.moraleForDefeatedBoss(fight.getBossCatalogEntry().getCategory())
                : ruleset.moraleForSurvivingBoss())
            .orElse(0.0);

        return new ColonyWeekOutcome(materials, moraleDelta);
    }

    /**
     * Returns the materials a week's completed challenges are worth, per player who completed them.
     *
     * @param weekStart Monday identifying the week
     * @return challenge materials
     */
    private int challengeMaterials(LocalDate weekStart) {
        return progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(weekStart)
            .stream()
            .filter(PlayerChallengeProgress::isCompleted)
            .mapToInt(progress -> ruleset.materialsForChallenge(
                progress.getWeeklyChallenge().getChallenge().getDifficulty()
            ))
            .sum();
    }
}
