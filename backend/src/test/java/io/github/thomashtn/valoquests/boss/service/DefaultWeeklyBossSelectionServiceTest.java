package io.github.thomashtn.valoquests.boss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests deterministic, non-repeating weekly boss selection and how a fight is sized.
 */
class DefaultWeeklyBossSelectionServiceTest {

    /** Week resolved from the fixed application clock. */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /** Roster size every fixture's run is frozen at, and therefore what its fights are sized on. */
    private static final int ACTIVE_PLAYERS = 7;

    /** Per-player output the calibration service is pinned to. */
    private static final int REFERENCE = 10_000;

    /** Barèmes the service under test is wired with. */
    private static final ScoringRuleset RULESET = new DefaultScoringRuleset();

    /** Identifier of the run every fixture's campaign runs in. */
    private static final long CAMPAIGN_RUN_ID = 42L;

    /** Run every fixture's campaign runs in. */
    private static final Run CAMPAIGN_RUN = campaignRun();

    /** Catalogue repository dependency. */
    private BossCatalogEntryRepository catalogRepository;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Calibration dependency, measuring what a player currently contributes. */
    private BossCalibrationService calibrationService;

    /** Campaign run dependency, naming the run a new fight is stamped with. */
    private RunService runService;

    /** Service under test. */
    private DefaultWeeklyBossSelectionService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        catalogRepository = mock(BossCatalogEntryRepository.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);
        calibrationService = mock(BossCalibrationService.class);
        runService = mock(RunService.class);

        lenient().when(calibrationService.referenceDamagePerPlayer()).thenReturn(REFERENCE);
        lenient().when(runService.ensureRunFor(any())).thenReturn(CAMPAIGN_RUN);
        lenient().when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(CAMPAIGN_RUN_ID))
            .thenReturn(List.of());
        lenient().when(encounterRepository.findByWeekStart(any())).thenReturn(Optional.empty());
        lenient().when(encounterRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Clock clock = Clock.fixed(Instant.parse("2026-07-21T10:00:00Z"), ZoneOffset.UTC);

        service = new DefaultWeeklyBossSelectionService(
            catalogRepository,
            encounterRepository,
            RULESET,
            calibrationService,
            new WeekCalendar(clock, ZoneOffset.UTC),
            runService
        );
    }

    /**
     * Builds the run every fixture's campaign runs in.
     *
     * @return persisted-looking run
     */
    private static Run campaignRun() {
        Run run = new Run();
        run.setId(CAMPAIGN_RUN_ID);
        run.setNumber(3);
        run.setFirstWeekStart(LocalDate.of(2026, 6, 1));
        run.setLastWeekStart(LocalDate.of(2026, 8, 3));
        run.setRosterSize(ACTIVE_PLAYERS);

        return run;
    }

    /**
     * Verifies that an existing selection is returned as-is, never replaced.
     */
    @Test
    void shouldNeverReplaceAnExistingSelection() {
        WeeklyBossEncounter existing = new WeeklyBossEncounter();
        existing.setWeekStart(WEEK_START);

        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.of(existing));

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result).isSameAs(existing);
        verify(encounterRepository, never()).save(any());
    }

    /**
     * Verifies that the boss cannot repeat before the whole catalogue has been drawn once.
     */
    @Test
    void shouldNotRepeatABossUntilTheCatalogueHasFullyCycled() {
        BossCatalogEntry bossA = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        BossCatalogEntry bossB = createBoss(2L, "BOSS_B", BossCategory.STANDARD);
        BossCatalogEntry bossC = createBoss(3L, "BOSS_C", BossCategory.STANDARD);

        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc())
            .thenReturn(List.of(bossA, bossB, bossC));

        // Two of the three bosses were already drawn in previous weeks of this act, still within one
        // cycle.
        when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(CAMPAIGN_RUN_ID))
            .thenReturn(List.of(
                createEncounter(WEEK_START.minusWeeks(2), bossA),
                createEncounter(WEEK_START.minusWeeks(1), bossB)
            ));

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        // Only bossC has not been drawn yet in the current cycle, so it is the only valid candidate.
        assertThat(result.getBossCatalogEntry()).isSameAs(bossC);
    }

    /**
     * Verifies that a new run restarts the no-repeat cycle rather than inheriting the bosses its
     * predecessor had already used.
     *
     * <p>A campaign opening on the one boss the previous run had not reached yet would face a
     * shrinking catalogue instead of a fresh run. The scoping query is what guarantees it: a previous
     * run's encounters simply are not returned for the run now in progress.
     */
    @Test
    void shouldRestartTheCycleWhenTheRunChanges() {
        BossCatalogEntry bossA = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        BossCatalogEntry bossB = createBoss(2L, "BOSS_B", BossCategory.STANDARD);
        BossCatalogEntry bossC = createBoss(3L, "BOSS_C", BossCategory.STANDARD);

        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc())
            .thenReturn(List.of(bossA, bossB, bossC));

        // Two bosses were drawn during the previous run, none during the one now in progress. Read
        // over the whole history, bossC would be the only candidate left.
        when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(CAMPAIGN_RUN_ID))
            .thenReturn(List.of());

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        // The whole catalogue is available again, so the draw is free rather than forced onto the one
        // boss the previous run had not reached.
        assertThat(result.getBossCatalogEntry()).isNotSameAs(bossC);
        assertThat(result.getRun()).isSameAs(CAMPAIGN_RUN);
    }

    /**
     * Verifies that a fight records the run it belongs to, which is what scopes the campaign to it.
     */
    @Test
    void shouldStampTheFightWithTheRunInProgress() {
        givenSingleBoss(BossCategory.STANDARD);

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getRun()).isSameAs(CAMPAIGN_RUN);
    }

    /**
     * Verifies that re-sizing an open fight leaves the run it was stamped with untouched.
     *
     * <p>Unlike the act it replaces, a run is resolved from the week's own date, so it is right the
     * moment the fight is drawn and re-attaching it could only ever move it somewhere wrong.
     */
    @Test
    void shouldNotMoveTheRunOfAnOpenFightWhenResizing() {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        Run originalRun = campaignRun();
        WeeklyBossEncounter existing = createEncounter(WEEK_START, boss);
        existing.setRun(originalRun);

        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.of(existing));

        Optional<WeeklyBossEncounter> result = service.resizeWeekBoss(WEEK_START);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getRun()).isSameAs(originalRun);
    }

    /**
     * Verifies that a completed cycle resets and lets any boss be drawn again.
     */
    @Test
    void shouldAllowRepetitionOnceTheCatalogueCycleCompletes() {
        BossCatalogEntry bossA = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        BossCatalogEntry bossB = createBoss(2L, "BOSS_B", BossCategory.STANDARD);

        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc())
            .thenReturn(List.of(bossA, bossB));

        // Both bosses of this two-entry catalogue were already drawn: the cycle is complete, so the
        // next draw must be free to pick from the full catalogue again.
        when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(CAMPAIGN_RUN_ID))
            .thenReturn(List.of(
                createEncounter(WEEK_START.minusWeeks(2), bossA),
                createEncounter(WEEK_START.minusWeeks(1), bossB)
            ));

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getBossCatalogEntry()).isIn(bossA, bossB);
    }

    /**
     * Verifies that a fight is the measured reference times the roster, weighted by the boss category,
     * and that the roster it was sized for is recorded alongside it.
     */
    @Test
    void shouldSizeTheFightOnTheRosterAndTheMeasuredReference() {
        givenSingleBoss(BossCategory.STANDARD);

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getEffectiveHp())
            .isEqualTo(RULESET.bossHitPoints(BossCategory.STANDARD, ACTIVE_PLAYERS, REFERENCE));
        assertThat(result.getActivePlayerCount()).isEqualTo(ACTIVE_PLAYERS);

        ArgumentCaptor<WeeklyBossEncounter> captor =
            ArgumentCaptor.forClass(WeeklyBossEncounter.class);
        verify(encounterRepository).save(captor.capture());
        assertThat(captor.getValue().getWeekStart()).isEqualTo(WEEK_START);
    }

    /**
     * Verifies that the fight follows the measurement rather than a constant.
     */
    @Test
    void shouldFollowTheMeasuredReference() {
        givenSingleBoss(BossCategory.STANDARD);
        when(calibrationService.referenceDamagePerPlayer()).thenReturn(REFERENCE / 2);

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getEffectiveHp())
            .isEqualTo(RULESET.bossHitPoints(BossCategory.STANDARD, ACTIVE_PLAYERS, REFERENCE / 2));
    }

    /**
     * Verifies that an absent player still weighs on the fight, and that the count doing the weighing is
     * the roster <b>frozen on the run</b>.
     *
     * <p>The roster is what sizes the boss, not attendance: a player left active and away all week adds
     * their share of hit points without dealing any, which is what makes turning up a collective
     * commitment. Deactivating them through the backoffice is the supported way to shrink a week, and it
     * takes effect on the next run rather than in the middle of this one — with two different counts in
     * one model, deactivating somebody in week eight made week nine's fight easier without taking
     * anything off the materials it pays.
     */
    @Test
    void shouldSizeOnTheRunsFrozenRosterRatherThanOnAttendance() {
        givenSingleBoss(BossCategory.STANDARD);

        WeeklyBossEncounter sevenPlayers = service.selectWeekBoss(WEEK_START);

        Run shrunkRun = campaignRun();
        shrunkRun.setRosterSize(6);
        when(runService.ensureRunFor(any())).thenReturn(shrunkRun);
        WeeklyBossEncounter sixPlayers = service.selectWeekBoss(WEEK_START);

        assertThat(sevenPlayers.getEffectiveHp())
            .isEqualTo(RULESET.bossHitPoints(BossCategory.STANDARD, ACTIVE_PLAYERS, REFERENCE));
        assertThat(sixPlayers.getEffectiveHp()).isLessThan(sevenPlayers.getEffectiveHp());
    }

    /**
     * Verifies that re-sizing an open week applies the roster and reference as they now stand, keeping
     * the boss that was already drawn.
     */
    @Test
    void shouldResizeAnOpenWeekWithoutRedrawingItsBoss() {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", BossCategory.STANDARD);

        WeeklyBossEncounter current = createEncounter(WEEK_START, boss);
        current.setEffectiveHp(1);
        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.of(current));

        Optional<WeeklyBossEncounter> result = service.resizeWeekBoss(WEEK_START);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getBossCatalogEntry()).isSameAs(boss);
        assertThat(result.orElseThrow().getEffectiveHp())
            .isEqualTo(RULESET.bossHitPoints(BossCategory.STANDARD, ACTIVE_PLAYERS, REFERENCE));
        verify(encounterRepository).save(current);
    }

    /**
     * Verifies that a finalized week is never re-sized: its fight has already been judged.
     */
    @Test
    void shouldNotResizeAFinalizedWeek() {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", BossCategory.STANDARD);

        WeeklyBossEncounter current = createEncounter(WEEK_START, boss);
        current.setFinalizedAt(Instant.parse("2026-07-20T00:05:00Z"));
        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.of(current));

        assertThat(service.resizeWeekBoss(WEEK_START)).isEmpty();
        verify(encounterRepository, never()).save(any());
    }

    /** Registers a one-entry catalogue and returns the boss every draw resolves to. */
    private BossCatalogEntry givenSingleBoss(BossCategory category) {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", category);
        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(boss));
        return boss;
    }

    /** Creates a catalogue boss fixture. */
    private BossCatalogEntry createBoss(Long id, String code, BossCategory category) {
        BossCatalogEntry boss = new BossCatalogEntry();
        boss.setId(id);
        boss.setCode(code);
        boss.setName(code);
        boss.setDescription("Fixture boss " + code);
        boss.setCategory(category);
        boss.setEnabled(true);
        return boss;
    }

    /** Creates an open encounter fixture referencing a drawn boss and the run it was stamped with. */
    private WeeklyBossEncounter createEncounter(LocalDate weekStart, BossCatalogEntry boss) {
        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(weekStart);
        encounter.setBossCatalogEntry(boss);
        encounter.setRun(CAMPAIGN_RUN);
        return encounter;
    }
}
