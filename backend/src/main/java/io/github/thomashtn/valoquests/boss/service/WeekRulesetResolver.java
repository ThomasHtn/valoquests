package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRulesetRegistry;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the scoring ruleset a week must use, from the ruleset version its own boss encounter was
 * created with.
 *
 * <p>Lazily draws the current week's encounter when it does not exist yet, mirroring how weekly
 * challenges are already selected rather than loaded elsewhere in this codebase: normally the rollover
 * already created it when the week opened, so this only matters for the week in progress when this
 * feature is first deployed, or after a manual recalculation trigger. It never does so for a past week,
 * which would rewrite which boss that week fought.
 */
@Component
public class WeekRulesetResolver {

    /**
     * Repository used to read the week's own boss encounter.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Service used to lazily draw the current week's boss encounter.
     */
    private final WeeklyBossSelectionService bossSelectionService;

    /**
     * Registry resolving a ruleset from a persisted version, or the current one.
     */
    private final ScoringRulesetRegistry rulesetRegistry;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the week ruleset resolver.
     *
     * @param encounterRepository weekly boss encounter repository
     * @param bossSelectionService boss selection service
     * @param rulesetRegistry     scoring ruleset registry
     * @param weekCalendar        calendar resolving the current week
     */
    public WeekRulesetResolver(
        WeeklyBossEncounterRepository encounterRepository,
        WeeklyBossSelectionService bossSelectionService,
        ScoringRulesetRegistry rulesetRegistry,
        WeekCalendar weekCalendar
    ) {
        this.encounterRepository = encounterRepository;
        this.bossSelectionService = bossSelectionService;
        this.rulesetRegistry = rulesetRegistry;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Resolves the ruleset a week must use.
     *
     * @param weekStart week being resolved
     * @return ruleset to resolve every damage amount with
     */
    @Transactional
    public ScoringRuleset resolve(LocalDate weekStart) {
        Optional<WeeklyBossEncounter> existing = encounterRepository.findByWeekStart(weekStart);

        if (existing.isPresent()) {
            return rulesetRegistry.forVersion(existing.orElseThrow().getRulesetVersion());
        }

        if (weekStart.equals(weekCalendar.currentWeekStart())) {
            WeeklyBossEncounter created = bossSelectionService.selectWeekBoss(weekStart);
            return rulesetRegistry.forVersion(created.getRulesetVersion());
        }

        return rulesetRegistry.current();
    }
}
