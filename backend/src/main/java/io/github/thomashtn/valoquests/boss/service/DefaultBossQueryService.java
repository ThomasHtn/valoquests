package io.github.thomashtn.valoquests.boss.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.boss.dto.BossHistoryWeekResponse;
import io.github.thomashtn.valoquests.boss.dto.BossResponse;
import io.github.thomashtn.valoquests.boss.dto.CurrentBossResponse;
import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides optimized read-only access to the current and historical boss confrontations.
 */
@Service
@Transactional(readOnly = true)
public class DefaultBossQueryService implements BossQueryService {

    /**
     * Service used to draw or retrieve the current week's boss encounter.
     *
     * <p>Selection is idempotent and only ever creates something for the current week, never for a
     * past one, mirroring how the equivalent challenge and ranking queries already rely on lazy
     * selection elsewhere in this codebase.
     */
    private final WeeklyBossSelectionService weeklyBossSelectionService;

    /**
     * Repository used to read historical boss encounters.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Repository used to read weekly player scores, to sum the week's total damage dealt.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Resolves the run the campaign currently runs in.
     */
    private final RunService runService;

    /**
     * Ruleset supplying how many weeks a run spans.
     */
    private final ColonyRuleset colonyRuleset;

    /**
     * Creates the boss query service.
     *
     * @param weeklyBossSelectionService boss selection service
     * @param encounterRepository        weekly boss encounter repository
     * @param scoreRepository            weekly player score repository
     * @param weekCalendar               calendar resolving the current week
     * @param runService                 service resolving the run the campaign runs in
     * @param colonyRuleset              ruleset supplying the run length
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultBossQueryService(
        WeeklyBossSelectionService weeklyBossSelectionService,
        WeeklyBossEncounterRepository encounterRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        WeekCalendar weekCalendar,
        RunService runService,
        ColonyRuleset colonyRuleset
    ) {
        this.weeklyBossSelectionService = weeklyBossSelectionService;
        this.encounterRepository = encounterRepository;
        this.scoreRepository = scoreRepository;
        this.weekCalendar = weekCalendar;
        this.runService = runService;
        this.colonyRuleset = colonyRuleset;
    }

    @Override
    @Transactional
    public CurrentBossResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        WeeklyBossEncounter encounter = weeklyBossSelectionService.selectCurrentWeekBoss();
        Run run = encounter.getRun();

        return new CurrentBossResponse(
            weekStart,
            weekStart.plusDays(6),
            toBossResponse(encounter.getBossCatalogEntry()),
            encounter.getEffectiveHp(),
            totalDamageDealt(weekStart),
            run.getNumber(),
            weekIndexInRun(run, weekStart),
            colonyRuleset.runLengthWeeks()
        );
    }

    /**
     * Places one week inside its run, counting from one.
     *
     * @param run       run the week belongs to
     * @param weekStart Monday identifying the week
     * @return the week's one-based position in the run
     */
    private int weekIndexInRun(Run run, LocalDate weekStart) {
        return (int) ChronoUnit.WEEKS.between(run.getFirstWeekStart(), weekStart) + 1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Scoped to the run the campaign currently runs in. The campaign is the ten fights of one run,
     * not an ever-growing timeline: a new run therefore opens on an empty history and the map starts
     * again from the week being fought. Nothing is deleted — a closed run's fights keep their rows and
     * simply stop being the campaign.
     *
     * <p>An empty page is returned while no run has been opened, which is the state of a deployment
     * that has not seen its first rollover: there is no campaign yet to show.
     */
    @Override
    public PageResponse<BossHistoryWeekResponse> findHistory(int page, int size) {
        PaginationGuard.assertValidPageRequest(page, size);

        Page<WeeklyBossEncounter> encounterPage = runService.currentRunId()
            .map(runId -> encounterRepository
                .findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartDesc(
                    runId,
                    PageRequest.of(page, size)
                ))
            .orElseGet(() -> Page.empty(PageRequest.of(page, size)));

        return new PageResponse<>(
            encounterPage.getContent().stream().map(this::toHistoryWeek).toList(),
            encounterPage.getNumber(),
            encounterPage.getSize(),
            encounterPage.getTotalElements(),
            encounterPage.getTotalPages()
        );
    }

    /**
     * Sums the damage dealt to the boss so far during a week still in progress.
     *
     * <p>A player's total damage minus their regularity bonus, which is the one component that stays
     * out of the fight: it rewards showing up rather than output. The team bonus is retroactive and
     * identical for everyone who completed a challenge, so this sum is the same number
     * {@code BossChronologyService} arrives at when it walks the week in order at closure. It has to
     * be, or the health bar would show progress the fight never recognised.
     *
     * <p>A showcased non-competitive player's damage is never counted against the boss.
     *
     * @param weekStart week being queried
     * @return cumulative damage dealt
     */
    private int totalDamageDealt(LocalDate weekStart) {
        return scoreRepository.findAllByWeekStartOrderByPositionAsc(weekStart)
            .stream()
            .filter(score -> score.getPlayer().isCompetitive())
            .mapToInt(score -> score.getTotalDamage() - score.getRegularityBonus())
            .sum();
    }

    /**
     * Maps one finalized encounter to its immutable history representation.
     *
     * <p>Damage is read from the encounter, where closure froze it, rather than recomputed from the
     * week's scores: a past week's fight is settled, and an admin recalculating that week must not be
     * able to move a number a later week already inherited from.
     *
     * @param encounter finalized boss encounter
     * @return history entry
     */
    private BossHistoryWeekResponse toHistoryWeek(WeeklyBossEncounter encounter) {
        Player defeatedByPlayer = encounter.getDefeatedByPlayer();

        return new BossHistoryWeekResponse(
            encounter.getWeekStart(),
            encounter.getWeekStart().plusDays(6),
            encounter.getFinalizedAt(),
            toBossResponse(encounter.getBossCatalogEntry()),
            encounter.getEffectiveHp(),
            encounter.getDamageDealt(),
            encounter.isDefeated(),
            defeatedByPlayer == null ? null : defeatedByPlayer.getId(),
            defeatedByPlayer == null ? null : defeatedByPlayer.getDisplayName()
        );
    }

    /**
     * Maps one catalogue entry to its API representation.
     *
     * @param catalogEntry drawn boss
     * @return boss identity response
     */
    private BossResponse toBossResponse(BossCatalogEntry catalogEntry) {
        return new BossResponse(
            catalogEntry.getCode(),
            catalogEntry.getName(),
            catalogEntry.getDescription(),
            catalogEntry.getImageUrl(),
            catalogEntry.getCategory()
        );
    }
}
