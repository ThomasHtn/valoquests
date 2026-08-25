package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds up the materials one finished week brings in.
 *
 * <p>Both sources are credited at the weekly rollover, in one go, once the week is finalized and both
 * the completions and the fight's outcome are known. Attributing each challenge to the exact day it was
 * validated would have meant replaying the progress calculators day by day, on a subsystem that has
 * never had that notion — by far the most expensive and most fragile piece of the design, for the sole
 * comfort of seeing materials on a Tuesday rather than the following Monday.
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
     * Ruleset pricing a completed challenge and a defeated boss.
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
     * Returns the materials a week is worth, challenges and boss together.
     *
     * <p>A week with no encounter at all is simply a week with no boss materials. That is what keeps a
     * run ten weeks long instead of ten fights long, and therefore what keeps runs comparable.
     *
     * @param weekStart Monday identifying the finished week
     * @return materials to credit
     */
    public int materialsOf(LocalDate weekStart) {
        return challengeMaterials(weekStart) + bossMaterials(weekStart);
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

    /**
     * Returns the materials a week's fight is worth.
     *
     * <p>Only a fight that was both finalized and won pays. A surviving boss brings nothing; that is its
     * entire cost, and the reason all ten fights of a run weigh the same on the final score.
     *
     * @param weekStart Monday identifying the week
     * @return boss materials
     */
    private int bossMaterials(LocalDate weekStart) {
        return encounterRepository.findByWeekStart(weekStart)
            .filter(encounter -> encounter.getFinalizedAt() != null)
            .filter(WeeklyBossEncounter::isDefeated)
            .map(encounter -> ruleset.materialsPerDefeatedBoss())
            .orElse(0);
    }
}
