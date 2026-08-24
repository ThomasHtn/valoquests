package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Measures what one player currently contributes to a fight in a week, from the weeks already closed.
 *
 * <p>This is what makes the boss self-calibrating. Sizing a fight from a constant only works while the
 * roster's habits match whatever the constant was written for: chosen from a busy month it makes every
 * quiet month unwinnable, chosen from a quiet one it makes the fight a formality. The difficulty
 * modifier cannot rescue either case, since it only travels thirty percent either way.
 *
 * <p>The reference is the <em>median</em> per-player output of the recent finalized weeks, not their
 * mean: one marathon week from a single player would drag a mean up and quietly raise the bar for
 * everyone else, which is the farm incentive the whole barème is built to avoid.
 */
@Service
public class BossCalibrationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BossCalibrationService.class);

    /**
     * Number of closed weeks below which the seed is used unchanged.
     *
     * <p>One week is an anecdote. Two is the least that can disagree, and the median of two is their
     * midpoint, which is already a fairer bar than either taken alone.
     */
    private static final int MINIMUM_WEEKS_TO_MEASURE = 2;

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * Repository used to read the weeks calibration is measured from.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Barèmes owning the seed, the window and the band.
     */
    private final ScoringRuleset ruleset;

    /**
     * Creates the boss calibration service.
     *
     * @param encounterRepository weekly boss encounter repository
     * @param ruleset             scoring ruleset
     */
    public BossCalibrationService(
        WeeklyBossEncounterRepository encounterRepository,
        ScoringRuleset ruleset
    ) {
        this.encounterRepository = encounterRepository;
        this.ruleset = ruleset;
    }

    /**
     * Returns the per-player weekly output the next fight must be sized against.
     *
     * @return measured reference, clamped to the ruleset's band, or its seed while history is too thin
     */
    @Transactional(readOnly = true)
    public int referenceDamagePerPlayer() {
        int seed = ruleset.seedReferenceDamagePerPlayer();

        List<WeeklyBossEncounter> recentWeeks = encounterRepository.findRecentFinalized(
            PageRequest.of(0, ruleset.calibrationWindowWeeks())
        );

        if (recentWeeks.size() < MINIMUM_WEEKS_TO_MEASURE) {
            LOGGER.debug(
                "Boss calibration falls back on the seed reference {}: only {} closed week(s).",
                seed,
                recentWeeks.size()
            );

            return seed;
        }

        int measured = median(recentWeeks.stream()
            .mapToInt(this::damagePerPlayer)
            .sorted()
            .toArray());

        int floor = (int) Math.round(seed * ruleset.calibrationFloorPercent() / PERCENT_SCALE);
        int ceiling = (int) Math.round(seed * ruleset.calibrationCeilingPercent() / PERCENT_SCALE);
        int clamped = Math.clamp(measured, floor, ceiling);

        LOGGER.info(
            "Boss calibration measured {} damage per player over {} closed week(s), applied as {}.",
            measured,
            recentWeeks.size(),
            clamped
        );

        return clamped;
    }

    /**
     * Returns what one player contributed during one closed week.
     *
     * @param encounter finalized encounter recording its own roster size
     * @return damage dealt per active player
     */
    private int damagePerPlayer(WeeklyBossEncounter encounter) {
        return encounter.getDamageDealt() / Math.max(1, encounter.getActivePlayerCount());
    }

    /**
     * Returns the median of an ascending array, averaging the middle pair when it has even length.
     *
     * @param ascendingValues values in ascending order, never empty
     * @return median value
     */
    private int median(int[] ascendingValues) {
        int middle = ascendingValues.length / 2;

        if (ascendingValues.length % 2 == 1) {
            return ascendingValues[middle];
        }

        return (ascendingValues[middle - 1] + ascendingValues[middle]) / 2;
    }
}
