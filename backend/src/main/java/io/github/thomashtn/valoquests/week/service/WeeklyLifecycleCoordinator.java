package io.github.thomashtn.valoquests.week.service;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.boss.service.BossChronologyResult;
import io.github.thomashtn.valoquests.boss.service.BossChronologyService;
import io.github.thomashtn.valoquests.boss.service.WeeklyBossSelectionService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRulesetRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates opening a new week and closing the previous week's boss encounter, on behalf of
 * {@link DefaultWeeklyRolloverService}.
 *
 * <p>Exists to keep that service's own constructor within this codebase's parameter-count limit: it
 * already sits at the practical maximum coordinating challenge and ranking recalculation, so the
 * boss-specific collaborators this feature adds are grouped here instead of inflating it further.
 */
@Service
public class WeeklyLifecycleCoordinator {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyLifecycleCoordinator.class);

    /**
     * Service used to prepare the new week's challenge pack.
     */
    private final WeeklyChallengeSelectionService weeklyChallengeSelectionService;

    /**
     * Service used to draw the new week's boss encounter.
     */
    private final WeeklyBossSelectionService weeklyBossSelectionService;

    /**
     * Repository used to load and finalize the closing week's boss encounter.
     */
    private final WeeklyBossEncounterRepository bossEncounterRepository;

    /**
     * Service used to determine whether the closing week's boss was defeated, and by whom.
     */
    private final BossChronologyService bossChronologyService;

    /**
     * Registry used to resolve the closing week's own ruleset version.
     */
    private final ScoringRulesetRegistry rulesetRegistry;

    /**
     * Creates the weekly lifecycle coordinator.
     *
     * @param weeklyChallengeSelectionService challenge selection service
     * @param weeklyBossSelectionService      boss selection service
     * @param bossEncounterRepository         weekly boss encounter repository
     * @param bossChronologyService           boss chronology service
     * @param rulesetRegistry                 scoring ruleset registry
     */
    public WeeklyLifecycleCoordinator(
        WeeklyChallengeSelectionService weeklyChallengeSelectionService,
        WeeklyBossSelectionService weeklyBossSelectionService,
        WeeklyBossEncounterRepository bossEncounterRepository,
        BossChronologyService bossChronologyService,
        ScoringRulesetRegistry rulesetRegistry
    ) {
        this.weeklyChallengeSelectionService = weeklyChallengeSelectionService;
        this.weeklyBossSelectionService = weeklyBossSelectionService;
        this.bossEncounterRepository = bossEncounterRepository;
        this.bossChronologyService = bossChronologyService;
        this.rulesetRegistry = rulesetRegistry;
    }

    /**
     * Prepares the challenge pack and boss encounter for a new week.
     *
     * @param weekStart Monday identifying the new week
     */
    @Transactional
    public void openWeek(LocalDate weekStart) {
        weeklyChallengeSelectionService.selectWeekChallenges(weekStart);
        weeklyBossSelectionService.selectWeekBoss(weekStart);
    }

    /**
     * Finalizes the closing week's boss encounter, when one exists and is not already finalized.
     *
     * <p>Resolves the chronology using the ruleset version and effective hit points frozen when the
     * encounter was created — never the currently registered ruleset, so a later barème adjustment
     * cannot rewrite this week's outcome. Absent entirely for a week that predates this feature, in
     * which case there is nothing to close.
     *
     * @param weekStart   week being closed
     * @param finalizedAt shared finalization timestamp
     */
    @Transactional
    public void closeBossEncounterIfNeeded(LocalDate weekStart, Instant finalizedAt) {
        Optional<WeeklyBossEncounter> existingEncounter = bossEncounterRepository.findByWeekStart(weekStart);

        if (existingEncounter.isEmpty()) {
            LOGGER.info(
                "No boss encounter exists for previous week {}. Nothing needs to be finalized.",
                weekStart
            );

            return;
        }

        WeeklyBossEncounter encounter = existingEncounter.orElseThrow();

        if (encounter.getFinalizedAt() != null) {
            LOGGER.info("Boss encounter for previous week {} is already finalized.", weekStart);

            return;
        }

        ScoringRuleset ruleset = rulesetRegistry.forVersion(encounter.getRulesetVersion());

        BossChronologyResult chronologyResult = bossChronologyService.computeChronology(
            weekStart,
            ruleset,
            encounter.getEffectiveHp()
        );

        encounter.setDefeated(chronologyResult.defeated());
        encounter.setDefeatedByPlayer(chronologyResult.defeatedByPlayer());
        encounter.setFinishingPlayerMatch(chronologyResult.finishingPlayerMatch());
        encounter.setDamageDealt(chronologyResult.totalDamage());
        encounter.setFinalizedAt(finalizedAt);

        bossEncounterRepository.save(encounter);

        LOGGER.info(
            "Boss encounter finalized for previous week {}: defeated={}, damageDealt={}, remainingHp={}.",
            weekStart,
            chronologyResult.defeated(),
            chronologyResult.totalDamage(),
            encounter.remainingHp()
        );
    }
}
