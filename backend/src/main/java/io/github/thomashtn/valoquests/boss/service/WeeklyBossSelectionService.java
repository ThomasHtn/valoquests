package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Creates and retrieves the deterministic weekly boss encounter.
 */
public interface WeeklyBossSelectionService {

    /**
     * Selects the boss encounter for the current week.
     *
     * @return current week's encounter
     */
    WeeklyBossEncounter selectCurrentWeekBoss();

    /**
     * Retrieves or creates the boss encounter assigned to one week.
     *
     * @param weekStart Monday identifying the week
     * @return that week's encounter
     */
    WeeklyBossEncounter selectWeekBoss(LocalDate weekStart);

    /**
     * Sizes an existing, still-open encounter again from its immediate predecessor.
     *
     * <p>The encounter's hit points depend on how the previous week ended, but the boss page draws the
     * current week's encounter as soon as anyone opens it — which on a Monday can happen before that
     * Monday's rollover has closed the previous week. The draw is preserved and only the hit points are
     * recomputed, so the fight is sized against a settled predecessor rather than against whichever
     * state the chain happened to be in when someone first loaded the page.
     *
     * <p>Does nothing when the week owns no encounter, or owns one that is already finalized: a closed
     * fight is settled and later weeks may already have inherited from it.
     *
     * @param weekStart Monday identifying the week
     * @return the re-sized encounter, empty when there was nothing to re-size
     */
    Optional<WeeklyBossEncounter> resizeWeekBoss(LocalDate weekStart);

    /**
     * Retrieves the boss encounter a week already owns, creating nothing.
     *
     * @param weekStart Monday identifying the week
     * @return the week's encounter, empty when it never had one
     */
    Optional<WeeklyBossEncounter> findExistingWeekBoss(LocalDate weekStart);
}
