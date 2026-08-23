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
     * Retrieves the boss encounter a week already owns, creating nothing.
     *
     * @param weekStart Monday identifying the week
     * @return the week's encounter, empty when it never had one
     */
    Optional<WeeklyBossEncounter> findExistingWeekBoss(LocalDate weekStart);
}
