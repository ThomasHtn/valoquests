package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.colony.dto.ColonyResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyRunHistoryResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTrajectoryResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyUpkeepResponse;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyBuilding;
import io.github.thomashtn.valoquests.colony.model.ColonyGauge;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how the colony reads off its snapshots.
 */
class ColonyQueryServiceTest {

    /** Monday the fixture run opens on. */
    private static final LocalDate FIRST_WEEK = LocalDate.of(2026, 6, 1);

    /** Identifier of the fixture run. */
    private static final long RUN_ID = 1L;

    /** Scale a ratio is read as a percentage on. */
    private static final double PERCENT = 100.0;

    /** Run service dependency. */
    private RunService runService;

    /** Replay service dependency, only reached when a run has no snapshot yet. */
    private ColonyReplayService replayService;

    /** Snapshot repository dependency. */
    private ColonyDailySnapshotRepository snapshotRepository;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Calibration the thresholds are read from. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /** Service under test. */
    private ColonyQueryService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        runService = mock(RunService.class);
        replayService = mock(ColonyReplayService.class);
        snapshotRepository = mock(ColonyDailySnapshotRepository.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);

        lenient().when(runService.ensureRunFor(any())).thenReturn(run());
        lenient().when(encounterRepository
            .findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartAsc(RUN_ID))
            .thenReturn(List.of());

        Clock clock = Clock.fixed(Instant.parse("2026-06-10T09:00:00Z"), ZoneOffset.UTC);

        service = new ColonyQueryService(
            runService,
            replayService,
            snapshotRepository,
            encounterRepository,
            ruleset,
            new ColonyReplayEngine(ruleset),
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that the last snapshot is the colony's current state, placed inside its run.
     */
    @Test
    void shouldReadTodaysColonyOffTheLastSnapshot() {
        givenSnapshots(
            snapshot(FIRST_WEEK, 60, 60, 0, 400, 3_000, 12, 14),
            snapshot(FIRST_WEEK.plusDays(9), 72, 61, 4_300, 2_690, 4_200, 11.4, 10.0)
        );

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.runNumber()).isEqualTo(4);
        assertThat(colony.runDay()).isEqualTo(10);
        assertThat(colony.runDayCount()).isEqualTo(71);
        assertThat(colony.runWeekIndex()).isEqualTo(2);
        assertThat(colony.runWeekCount()).isEqualTo(10);
        assertThat(colony.population()).isEqualTo(2_690);
        assertThat(colony.capacity()).isEqualTo(4_200);
        assertThat(colony.materials()).isEqualTo(4_300);
        assertThat(colony.maximumCapacity()).isEqualTo(7_000);
        assertThat(colony.food().value()).isEqualTo(72.0);
        assertThat(colony.energy().value()).isEqualTo(61.0);
        assertThat(colony.food().gain()).isEqualTo(11.4);
        assertThat(colony.energy().gain()).isEqualTo(10.0);
    }

    /**
     * Verifies the upkeep the colony is about to pay, and what it takes to cover it.
     *
     * <p>Read against <b>today</b>, not the previous day: today's loss has already been charged and is
     * inside the value the gauge shows, so the only figure left to act on is the next one.
     */
    @Test
    void shouldReportTheUpcomingUpkeepAndWhatCoversIt() {
        givenSnapshots(
            snapshot(FIRST_WEEK, 60, 60, 0, 2_000, 4_000, 12, 14),
            snapshot(FIRST_WEEK.plusDays(1), 62, 58, 0, 2_100, 4_000, 12, 14)
        );

        ColonyResponse colony = service.findCurrent();

        // 14 x 2 100 / 4 000 = 7.35, both gauges alike.
        assertThat(colony.upkeep().upcomingLoss()).isEqualTo(7.35, within(1e-9));
        // Food: 7.35 x 400 = 2 940 damage, which is 2 940 / 425 = 6.9 competitive games.
        assertThat(colony.upkeep().damageToHold()).isEqualTo(2_940);
        assertThat(colony.upkeep().matchesToHold()).isEqualTo(7);
        // Energy: 7.35 x 7 / 14 = 3.675 players, so four of them have to turn up.
        assertThat(colony.upkeep().playersToHold()).isEqualTo(4);
        assertThat(colony.populationChange()).isEqualTo(100);
        assertThat(colony.dailyMigrationLimit()).isEqualTo(100);
    }

    /**
     * Verifies that a requirement is never rounded past the roster, nor asked of an empty colony.
     *
     * <p>An empty colony consumes nothing, so there is nothing to cover and no objective to state. At
     * the other end, a loss no turnout can absorb must still ask for the squad and not for eight of it.
     */
    @Test
    void shouldKeepTheUpkeepRequirementsWithinWhatCanBeAskedFor() {
        givenSnapshots(snapshot(FIRST_WEEK, 0, 0, 0, 0, 3_000, 0, 0));

        ColonyUpkeepResponse empty = service.findCurrent().upkeep();

        assertThat(empty.upcomingLoss()).isZero();
        assertThat(empty.damageToHold()).isZero();
        assertThat(empty.matchesToHold()).isZero();
        assertThat(empty.playersToHold()).isZero();

        givenSnapshots(snapshot(FIRST_WEEK, 100, 100, 0, 3_000, 3_000, 14, 14));

        assertThat(service.findCurrent().upkeep().playersToHold()).isEqualTo(7);
    }

    /**
     * Verifies that the gauge fed the least is named, along with the share of capacity it caps the
     * colony at.
     *
     * <p>The one property of the model that cannot be read off the gauges themselves.
     */
    @Test
    void shouldNameTheLimitingGaugeAndTheCeilingItSets() {
        givenSnapshots(snapshot(FIRST_WEEK, 100, 61, 0, 2_500, 4_200, 14.0, 8.0));

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.limitingGauge()).isEqualTo(ColonyGauge.ENERGY);
        assertThat(colony.equilibriumPercentage()).isEqualTo(8.0 / 14.0 * 100.0, within(1e-9));
    }

    /**
     * Verifies that the settling levels are resolved on the last complete days, never on today.
     *
     * <p>Today is a day in progress. Before anybody has played it, its gains are zero, and an
     * equilibrium read off it would sit at zero every morning and climb back through the evening — the
     * one reading a permanently displayed figure must never give.
     */
    @Test
    void shouldResolveTheSettlingLevelsOnTheLastCompleteDaysNotOnToday() {
        givenSnapshots(
            snapshot(FIRST_WEEK, 100, 40, 0, 1_700, 3_000, 14, 8),
            snapshot(FIRST_WEEK.plusDays(1), 100, 34, 0, 1_710, 3_000, 14, 8),
            snapshot(FIRST_WEEK.plusDays(2), 100, 33, 0, 1_714, 3_000, 14, 8),
            // Today, before a single game has been played on it.
            snapshot(FIRST_WEEK.plusDays(3), 92, 25, 0, 1_714, 3_000, 0, 0)
        );

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.limitingGauge()).isEqualTo(ColonyGauge.ENERGY);
        assertThat(colony.equilibriumPercentage()).isEqualTo(8.0 / 14.0 * PERCENT, within(0.5));
        // Food outproduces the loss and saturates; Energy is the one that settles, at 100 x health².
        assertThat(colony.food().equilibrium()).isEqualTo(100.0, within(1e-9));
        assertThat(colony.energy().equilibrium())
            .isEqualTo(PERCENT * Math.pow(8.0 / 14.0, 2), within(0.5));
    }

    /**
     * Verifies that Food is named when it is the one falling behind.
     */
    @Test
    void shouldNameFoodWhenItIsTheOneFallingBehind() {
        givenSnapshots(snapshot(FIRST_WEEK, 40, 90, 0, 1_000, 3_000, 4.0, 14.0));

        assertThat(service.findCurrent().limitingGauge()).isEqualTo(ColonyGauge.FOOD);
    }

    /**
     * Verifies that health is the geometric mean and that distress is flagged under a quarter of it.
     */
    @Test
    void shouldFlagDistressUnderAQuarterOfHealth() {
        givenSnapshots(snapshot(FIRST_WEEK, 100, 4, 0, 200, 3_000, 14.0, 1.0));

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.healthPercentage()).isEqualTo(20.0, within(1e-9));
        assertThat(colony.alert()).isTrue();
    }

    /**
     * Verifies that a healthy colony is not flagged.
     */
    @Test
    void shouldNotFlagAHealthyColony() {
        givenSnapshots(snapshot(FIRST_WEEK, 72, 61, 0, 2_690, 4_200, 11.4, 10.0));

        assertThat(service.findCurrent().alert()).isFalse();
    }

    /**
     * Verifies that every tier is reported, with the day the run reached it.
     */
    @Test
    void shouldReportEveryTierAndTheDayItWentUp() {
        givenSnapshots(
            snapshot(FIRST_WEEK, 60, 60, 0, 400, 3_000, 12, 14),
            snapshot(FIRST_WEEK.plusDays(21), 60, 60, 2_600, 1_200, 4_200, 12, 14),
            snapshot(FIRST_WEEK.plusDays(28), 60, 60, 4_300, 1_800, 4_200, 12, 14)
        );

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.buildings()).hasSize(4);
        assertThat(colony.buildings().getFirst().building()).isEqualTo(ColonyBuilding.CAMP);
        assertThat(colony.buildings().getFirst().erected()).isTrue();
        assertThat(colony.buildings().getFirst().erectedOnRunDay()).isEqualTo(1);
        assertThat(colony.buildings().get(1).building()).isEqualTo(ColonyBuilding.BARRACKS);
        assertThat(colony.buildings().get(1).erected()).isTrue();
        assertThat(colony.buildings().get(1).erectedOnRunDay()).isEqualTo(22);
        assertThat(colony.buildings().get(2).erected()).isFalse();
        assertThat(colony.buildings().get(2).erectedOnRunDay()).isNull();
    }

    /**
     * Verifies the progress towards the tier the run is working on.
     */
    @Test
    void shouldReportProgressTowardsTheNextTier() {
        givenSnapshots(snapshot(FIRST_WEEK, 60, 60, 5_240, 2_000, 4_200, 12, 14));

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.nextTier().building()).isEqualTo(ColonyBuilding.RESIDENTIAL_QUARTER);
        assertThat(colony.nextTier().materialsThreshold()).isEqualTo(6_200);
        assertThat(colony.nextTier().missingMaterials()).isEqualTo(960);
        assertThat(colony.nextTier().progressPercentage())
            .isEqualTo(5_240 * 100.0 / 6_200, within(1e-9));
    }

    /**
     * Verifies that a run holding the Citadel has no tier left to work towards.
     */
    @Test
    void shouldReportNoNextTierOnceTheCitadelIsUp() {
        givenSnapshots(snapshot(FIRST_WEEK, 90, 90, 11_000, 6_000, 7_000, 14, 14));

        assertThat(service.findCurrent().nextTier()).isNull();
    }

    /**
     * Verifies that the defeated bosses of the run are counted, and only those.
     */
    @Test
    void shouldCountOnlyTheBossesTheRunPutDown() {
        givenSnapshots(snapshot(FIRST_WEEK, 60, 60, 0, 400, 3_000, 12, 14));
        when(encounterRepository.findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartAsc(RUN_ID))
            .thenReturn(List.of(encounter(true), encounter(false), encounter(true)));

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.defeatedBosses()).isEqualTo(2);
        assertThat(colony.bossCount()).isEqualTo(10);
    }

    /**
     * Verifies that a run with no snapshot is replayed once rather than failing.
     */
    @Test
    void shouldReplayARunThatHasNoSnapshotYet() {
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(RUN_ID))
            .thenReturn(List.of())
            .thenReturn(List.of(snapshot(FIRST_WEEK, 50, 50, 0, 375, 3_000, 14, 14)));

        ColonyResponse colony = service.findCurrent();

        verify(replayService).replay(any());
        assertThat(colony.population()).isEqualTo(375);
    }

    /**
     * Verifies that the curve carries its peak, its average and the days buildings went up.
     *
     * <p>The average is what separates a run that was held from a hollow one that ended at the same
     * place.
     */
    @Test
    void shouldReportThePeakTheAverageAndTheMilestones() {
        givenSnapshots(
            snapshot(FIRST_WEEK, 60, 60, 0, 1_000, 3_000, 12, 14),
            snapshot(FIRST_WEEK.plusDays(1), 60, 60, 2_500, 3_000, 4_200, 12, 14),
            snapshot(FIRST_WEEK.plusDays(2), 60, 60, 2_500, 2_000, 4_200, 12, 14)
        );

        ColonyTrajectoryResponse trajectory = service.findTrajectory();

        assertThat(trajectory.peakPopulation()).isEqualTo(3_000);
        assertThat(trajectory.peakDay()).isEqualTo(FIRST_WEEK.plusDays(1));
        assertThat(trajectory.averagePopulation()).isEqualTo(2_000);
        assertThat(trajectory.points()).hasSize(3);
        assertThat(trajectory.points().getFirst().runDay()).isEqualTo(1);
        assertThat(trajectory.milestones()).singleElement()
            .satisfies(milestone -> {
                assertThat(milestone.building()).isEqualTo(ColonyBuilding.BARRACKS);
                assertThat(milestone.runDay()).isEqualTo(2);
                assertThat(milestone.capacity()).isEqualTo(4_200);
            });
    }

    /**
     * Verifies that a closed run's score is the population of its settlement day.
     */
    @Test
    void shouldScoreAClosedRunOnItsSettlementDay() {
        Run closed = run();
        closed.setClosedAt(Instant.parse("2026-08-10T00:05:00Z"));
        when(runService.closedRuns()).thenReturn(List.of(closed));

        givenSnapshots(
            snapshot(FIRST_WEEK, 60, 60, 0, 1_000, 3_000, 12, 14),
            snapshot(FIRST_WEEK.plusDays(69), 80, 80, 6_500, 6_040, 5_500, 12, 14),
            snapshot(FIRST_WEEK.plusDays(70), 80, 80, 6_500, 5_780, 5_500, 12, 14)
        );

        List<ColonyRunHistoryResponse> history = service.findHistory();

        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.runNumber()).isEqualTo(4);
            assertThat(entry.settlementDay()).isEqualTo(FIRST_WEEK.plusDays(70));
            assertThat(entry.finalPopulation()).isEqualTo(5_780);
            assertThat(entry.maximumPercentage()).isEqualTo(5_780 * 100.0 / 7_000, within(1e-9));
            assertThat(entry.peakPopulation()).isEqualTo(6_040);
            assertThat(entry.erectedBuildings()).isEqualTo(3);
            assertThat(entry.buildingCount()).isEqualTo(4);
        });
    }

    /**
     * Verifies that a closed run with no snapshot at all reports zeroes rather than failing.
     */
    @Test
    void shouldReportZeroesForAClosedRunWithNoSnapshot() {
        Run closed = run();
        closed.setClosedAt(Instant.parse("2026-08-10T00:05:00Z"));
        when(runService.closedRuns()).thenReturn(List.of(closed));
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(RUN_ID)).thenReturn(List.of());

        assertThat(service.findHistory()).singleElement().satisfies(entry -> {
            assertThat(entry.finalPopulation()).isZero();
            assertThat(entry.peakPopulation()).isZero();
            assertThat(entry.averagePopulation()).isZero();
            assertThat(entry.erectedBuildings()).isEqualTo(1);
        });
    }

    /**
     * Registers the run's snapshots.
     *
     * @param snapshots the run's days, oldest first
     */
    private void givenSnapshots(ColonyDailySnapshot... snapshots) {
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(RUN_ID)).thenReturn(List.of(snapshots));
    }

    /**
     * Builds the fixture run.
     *
     * @return a ten-week run opening on {@link #FIRST_WEEK}
     */
    private static Run run() {
        Run run = new Run();
        run.setId(RUN_ID);
        run.setNumber(4);
        run.setFirstWeekStart(FIRST_WEEK);
        run.setLastWeekStart(FIRST_WEEK.plusWeeks(9));
        run.setRosterSize(7);

        return run;
    }

    /**
     * Builds a finalized fight fixture.
     *
     * @param defeated whether the boss went down
     * @return the encounter
     */
    private static WeeklyBossEncounter encounter(boolean defeated) {
        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setDefeated(defeated);
        encounter.setFinalizedAt(Instant.parse("2026-06-08T00:05:00Z"));

        return encounter;
    }

    /**
     * Builds one snapshot fixture.
     *
     * @param day        calendar day
     * @param food       Food gauge
     * @param energy     Energy gauge
     * @param materials  cumulative materials
     * @param population population
     * @param capacity   capacity
     * @param foodGain   Food gained that day
     * @param energyGain Energy gained that day
     * @return the snapshot
     */
    private static ColonyDailySnapshot snapshot(
        LocalDate day,
        double food,
        double energy,
        int materials,
        double population,
        int capacity,
        double foodGain,
        double energyGain
    ) {
        ColonyDailySnapshot snapshot = new ColonyDailySnapshot();
        snapshot.setDay(day);
        snapshot.setFood(BigDecimal.valueOf(food));
        snapshot.setEnergy(BigDecimal.valueOf(energy));
        snapshot.setMaterials(materials);
        snapshot.setPopulation(BigDecimal.valueOf(population));
        snapshot.setCapacity(capacity);
        snapshot.setActivePlayerCount(7);
        snapshot.setFoodGain(BigDecimal.valueOf(foodGain));
        snapshot.setEnergyGain(BigDecimal.valueOf(energyGain));

        return snapshot;
    }
}
