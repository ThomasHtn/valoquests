package io.github.thomashtn.valoquests.week.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.boss.service.BossChronologyResult;
import io.github.thomashtn.valoquests.boss.service.BossChronologyService;
import io.github.thomashtn.valoquests.boss.service.WeeklyBossSelectionService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
     * Barèmes the closing week's chronology is replayed with.
     */
    private final ScoringRuleset ruleset;

    /**
     * Service opening and closing the ten-week runs the campaign is bounded by.
     */
    private final RunService runService;

    /**
     * Creates the weekly lifecycle coordinator.
     *
     * @param weeklyChallengeSelectionService challenge selection service
     * @param weeklyBossSelectionService      boss selection service
     * @param bossEncounterRepository         weekly boss encounter repository
     * @param bossChronologyService           boss chronology service
     * @param ruleset                         scoring ruleset
     * @param runService                      run lifecycle service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public WeeklyLifecycleCoordinator(
        WeeklyChallengeSelectionService weeklyChallengeSelectionService,
        WeeklyBossSelectionService weeklyBossSelectionService,
        WeeklyBossEncounterRepository bossEncounterRepository,
        BossChronologyService bossChronologyService,
        ScoringRuleset ruleset,
        RunService runService
    ) {
        this.weeklyChallengeSelectionService = weeklyChallengeSelectionService;
        this.weeklyBossSelectionService = weeklyBossSelectionService;
        this.bossEncounterRepository = bossEncounterRepository;
        this.bossChronologyService = bossChronologyService;
        this.ruleset = ruleset;
        this.runService = runService;
    }

    /**
     * Prepares the run, challenge pack and boss encounter for a new week.
     *
     * <p>Called by the rollover once every past week has been finalized, so the re-size here is what
     * settles a week whose encounter was already drawn by a page view before its predecessor closed.
     * Every call is idempotent, and the re-size preserves the boss that was drawn.
     *
     * <p>The run comes first because the encounter is stamped with it. It also catches up on its own:
     * the rollover finalizes every week it missed in one pass and then opens the current one once, so a
     * rollover firing after a long outage can be several runs behind.
     *
     * <p>Opens nothing at all when no campaign is running and an operator has turned automatic
     * renewal off — the gap that setting exists to leave open. Only guards the case where none is
     * open yet: a campaign already under way still runs its own ten weeks regardless of the setting,
     * which only ever decides whether a *new* one follows.
     *
     * @param weekStart Monday identifying the new week
     */
    @Transactional
    public void openWeek(LocalDate weekStart) {
        if (runService.currentRun().isEmpty() && !runService.isAutoRenewEnabled()) {
            LOGGER.info(
                "No campaign is running and automatic renewal is off: week {} opens nothing.",
                weekStart
            );

            return;
        }

        runService.ensureRunFor(weekStart);
        weeklyChallengeSelectionService.selectWeekChallenges(weekStart);
        weeklyBossSelectionService.selectWeekBoss(weekStart);
        weeklyBossSelectionService.resizeWeekBoss(weekStart);
    }

    /**
     * Resolves every past week's fight that is still open, oldest week first.
     *
     * <p>Driven by the encounters themselves rather than by the weeks the rollover happens to be
     * finalizing. Closure used to ride on the challenge pack — only a week the pack query reported as
     * still open ever had its fight resolved — so an encounter belonging to a week finalized without it
     * was never looked at again. Nothing downstream then knew the week had been fought: the campaign map
     * reads finalized encounters only and left the week locked as if it were still to come, and the
     * colony credits materials and morale from the same rows, so a boss that held cost no morale at all.
     *
     * <p>Called once the rollover has rebuilt and frozen every pending week, so each fight is resolved
     * against a ranking that already counts the matches imported just before it.
     *
     * @param currentWeekStart Monday identifying the week in progress, whose fight is still running
     * @param finalizedAt      shared finalization timestamp
     */
    @Transactional
    public void closePastBossEncounters(LocalDate currentWeekStart, Instant finalizedAt) {
        List<WeeklyBossEncounter> unresolved = bossEncounterRepository
            .findAllByFinalizedAtIsNullAndWeekStartLessThanOrderByWeekStartAsc(currentWeekStart);

        LOGGER.info(
            "{} past boss encounter(s) awaiting resolution before week {}.",
            unresolved.size(),
            currentWeekStart
        );

        unresolved.forEach(encounter -> closeBossEncounter(encounter, finalizedAt));
    }

    /**
     * Resolves one open encounter's fight and freezes its outcome.
     *
     * <p>The chronology is replayed against the hit points frozen when the encounter was created, so the
     * fight is judged on the target it opened with even if the roster has since changed.
     *
     * @param encounter   encounter to resolve, known to be open
     * @param finalizedAt shared finalization timestamp
     */
    private void closeBossEncounter(WeeklyBossEncounter encounter, Instant finalizedAt) {
        LocalDate weekStart = encounter.getWeekStart();

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
