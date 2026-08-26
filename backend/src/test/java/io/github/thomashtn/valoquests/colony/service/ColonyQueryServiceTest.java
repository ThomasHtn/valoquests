package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.colony.dto.ColonyResponse;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyPresenceState;
import io.github.thomashtn.valoquests.colony.model.ColonyTierName;
import io.github.thomashtn.valoquests.colony.model.ColonyTierState;
import io.github.thomashtn.valoquests.colony.model.ColonyWeekOutcomeState;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how the colony reads off its snapshots.
 *
 * <p>The fixture is the worked state of the interface document: run 3, day 26 of 71, a roster frozen at
 * seven, 3 050 materials and 440 food a week. Every figure in it recomposes, which is exactly what the
 * page has to be able to show, so a response that stops agreeing with it fails here first.
 */
class ColonyQueryServiceTest {

    /** Monday the fixture run opens on. */
    private static final LocalDate FIRST_WEEK = LocalDate.of(2026, 6, 1);

    /** Identifier of the fixture run. */
    private static final long RUN_ID = 1L;

    /** Day 26 of the run, the state the interface document is written around. */
    private static final LocalDate TODAY = FIRST_WEEK.plusDays(25);

    /** Tolerance for the double arithmetic. */
    private static final double TOLERANCE = 1e-6;

    /** Run service dependency. */
    private RunService runService;

    /** Snapshot repository dependency. */
    private ColonyDailySnapshotRepository snapshotRepository;

    /** Replay service dependency, only reached when a run has no snapshot yet. */
    private ColonyReplayService replayService;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Player repository dependency, which names the turnout. */
    private PlayerRepository playerRepository;

    /** Activity reader dependency, which says what each player brought in. */
    private ColonyActivityReader activityReader;

    /** Calibration the thresholds are read from. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /** Service under test. */
    private ColonyQueryService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        runService = mock(RunService.class);
        snapshotRepository = mock(ColonyDailySnapshotRepository.class);
        replayService = mock(ColonyReplayService.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);
        playerRepository = mock(PlayerRepository.class);
        activityReader = mock(ColonyActivityReader.class);

        lenient().when(runService.ensureRunFor(any())).thenReturn(run());
        lenient().when(encounterRepository
            .findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartAsc(RUN_ID))
            .thenReturn(List.of());
        lenient().when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(RUN_ID))
            .thenReturn(List.of());
        lenient().when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of());
        lenient().when(activityReader.readRawDamageByPlayer(any())).thenReturn(Map.of());

        WeekCalendar weekCalendar = new WeekCalendar(
            Clock.fixed(Instant.parse("2026-06-26T09:00:00Z"), ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        ColonyReplayEngine engine = new ColonyReplayEngine(ruleset);

        service = new ColonyQueryService(
            new ColonyRunReader(runService, replayService, snapshotRepository, weekCalendar),
            new ColonyPresenceReader(playerRepository, activityReader, ruleset, engine),
            encounterRepository,
            ruleset,
            engine
        );
    }

    /**
     * Verifies the last snapshot is the colony's current state, placed inside its run.
     */
    @Test
    void shouldReadTodaysColonyOffTheLastSnapshot() {
        givenTheWorkedState();

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.runNumber()).isEqualTo(3);
        assertThat(colony.runDay()).isEqualTo(26);
        assertThat(colony.runDayCount()).isEqualTo(71);
        assertThat(colony.runWeekIndex()).isEqualTo(4);
        assertThat(colony.runWeekCount()).isEqualTo(10);
        assertThat(colony.day()).isEqualTo(TODAY);
        assertThat(colony.population()).isEqualTo(2_400);
        assertThat(colony.populationChange()).isEqualTo(92);
        assertThat(colony.capacity()).isEqualTo(3_625);
        assertThat(colony.materials()).isEqualTo(3_050);
    }

    /**
     * Verifies the two ceilings are handed over together, along with what the town eats and what it has
     * left over. Those four figures are the whole of the food readout.
     */
    @Test
    void shouldHandOverBothCeilingsAndWhatSeparatesThem() {
        givenTheWorkedState();

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.foodStock()).isEqualTo(440.0, within(TOLERANCE));
        assertThat(colony.feedablePopulation()).isEqualTo(3_520);
        assertThat(colony.weeklyConsumption()).isEqualTo(300.0, within(TOLERANCE));
        assertThat(colony.weeklySurplus()).isEqualTo(140.0, within(TOLERANCE));
    }

    /**
     * Verifies a town eating more than it brings in reports no surplus rather than a negative one.
     */
    @Test
    void shouldNeverReportANegativeSurplus() {
        givenSnapshots(snapshot(TODAY, 100.0, 3_000.0, 0.0, 55.0, 3_050, 3_625));

        assertThat(service.findCurrent().weeklySurplus()).isZero();
    }

    /**
     * Verifies the morale is handed over with its floor, its ceiling and the speed it buys tonight.
     */
    @Test
    void shouldReportTheSpeedTheMoraleBuys() {
        givenTheWorkedState();

        assertThat(service.findCurrent().morale()).satisfies(morale -> {
            assertThat(morale.value()).isEqualTo(55.0, within(TOLERANCE));
            assertThat(morale.floor()).isEqualTo(1.0);
            assertThat(morale.ceiling()).isEqualTo(100.0);
            assertThat(morale.growthPercentPerNight()).isEqualTo(8.25, within(TOLERANCE));
        });
    }

    /**
     * Verifies the tier the town sits in, the one above it, and how far along it is.
     *
     * <p>The active step carries the town's <b>real</b> housing, not the threshold it crossed: a row
     * reading "Borough 3 500" over a bar at a quarter reads as progress towards a tier already earned.
     */
    @Test
    void shouldPlaceTheTownOnItsLadder() {
        givenTheWorkedState();

        ColonyResponse colony = service.findCurrent();

        assertThat(colony.tier().name()).isEqualTo(ColonyTierName.BOROUGH);
        assertThat(colony.tier().threshold()).isEqualTo(3_500);
        assertThat(colony.tier().state()).isEqualTo(ColonyTierState.CURRENT);
        assertThat(colony.nextTier().threshold()).isEqualTo(4_000);
        assertThat(colony.missingCapacity()).isEqualTo(375);
        assertThat(colony.tierProgressPercentage()).isEqualTo(25.0, within(TOLERANCE));
    }

    /**
     * Verifies the ladder window opens one step behind the town and four ahead of it, so the step being
     * paid for is never pushed off the bottom of the panel.
     */
    @Test
    void shouldWindowTheLadderAroundTheTown() {
        givenTheWorkedState();

        assertThat(service.findCurrent().ladder()).satisfiesExactly(
            step -> assertThat(step.threshold()).isEqualTo(3_000),
            step -> assertThat(step.threshold()).isEqualTo(3_500),
            step -> assertThat(step.threshold()).isEqualTo(4_000),
            step -> assertThat(step.threshold()).isEqualTo(4_500),
            step -> assertThat(step.threshold()).isEqualTo(5_000),
            step -> assertThat(step.threshold()).isEqualTo(5_500)
        );

        assertThat(service.findCurrent().ladder()).extracting("state").containsExactly(
            ColonyTierState.REACHED,
            ColonyTierState.CURRENT,
            ColonyTierState.LOCKED,
            ColonyTierState.LOCKED,
            ColonyTierState.LOCKED,
            ColonyTierState.LOCKED
        );
    }

    /**
     * Verifies every week of the run is listed with what its fight paid, in housing.
     *
     * <p>Housing rather than materials: materials are an intermediate currency the player never handles,
     * and housing is the only part of a fight's reward still standing on settlement day.
     */
    @Test
    void shouldPriceEveryFightInHousing() {
        givenTheWorkedState();
        when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(RUN_ID)).thenReturn(List.of(
            encounter(FIRST_WEEK, BossCategory.MINOR, true),
            encounter(FIRST_WEEK.plusWeeks(1), BossCategory.STANDARD, false),
            encounter(FIRST_WEEK.plusWeeks(2), BossCategory.STANDARD, true)
        ));

        assertThat(service.findCurrent().weeks()).hasSize(10).satisfies(weeks -> {
            assertThat(weeks.get(0).state()).isEqualTo(ColonyWeekOutcomeState.DEFEATED);
            assertThat(weeks.get(0).materials()).isEqualTo(420);
            assertThat(weeks.get(0).housingGain()).isEqualTo(210);
            assertThat(weeks.get(0).moraleDelta()).isEqualTo(10.0);

            assertThat(weeks.get(1).state()).isEqualTo(ColonyWeekOutcomeState.SURVIVED);
            assertThat(weeks.get(1).materials()).isZero();
            assertThat(weeks.get(1).housingGain()).isZero();
            assertThat(weeks.get(1).moraleDelta()).isEqualTo(-20.0);

            assertThat(weeks.get(2).state()).isEqualTo(ColonyWeekOutcomeState.DEFEATED);
            assertThat(weeks.get(2).housingGain()).isEqualTo(280);
            assertThat(weeks.get(2).moraleDelta()).isEqualTo(15.0);

            assertThat(weeks.get(3).state()).isEqualTo(ColonyWeekOutcomeState.CURRENT);
            assertThat(weeks.get(4).state()).isEqualTo(ColonyWeekOutcomeState.UPCOMING);
        });
    }

    /**
     * Verifies the fight under way is priced at what it would pay rather than at zero, so the tile shows
     * what is on the table instead of an empty promise.
     */
    @Test
    void shouldPriceTheFightUnderWayAtWhatItWouldPay() {
        givenTheWorkedState();
        when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(RUN_ID)).thenReturn(List.of(
            encounter(FIRST_WEEK.plusWeeks(3), BossCategory.ELITE, false, null)
        ));

        assertThat(service.findCurrent().weeks().get(3)).satisfies(week -> {
            assertThat(week.state()).isEqualTo(ColonyWeekOutcomeState.CURRENT);
            assertThat(week.category()).isEqualTo(BossCategory.ELITE);
            assertThat(week.housingGain()).isEqualTo(350);
            assertThat(week.moraleDelta()).isEqualTo(20.0);
        });
    }

    /**
     * Verifies a week already behind and still unsettled is not reported as the fight under way.
     *
     * <p>It happens when a Monday's rollover never fired. Reported as under way it put three tiles in
     * the "fighting now" state at once; and since it settled nothing, it has to be quoted at nothing
     * rather than at what a win would have paid.
     */
    @Test
    void shouldNotReportAStaleWeekAsTheFightUnderWay() {
        givenTheWorkedState();
        when(encounterRepository.findAllByRunIdOrderByWeekStartAsc(RUN_ID)).thenReturn(List.of(
            encounter(FIRST_WEEK, BossCategory.ELITE, true, null),
            encounter(FIRST_WEEK.plusWeeks(3), BossCategory.ELITE, false, null)
        ));

        assertThat(service.findCurrent().weeks()).satisfies(weeks -> {
            assertThat(weeks.get(0).state()).isEqualTo(ColonyWeekOutcomeState.SURVIVED);
            assertThat(weeks.get(0).materials()).isZero();
            assertThat(weeks.get(0).housingGain()).isZero();
            assertThat(weeks.get(0).moraleDelta()).isZero();

            // The week today falls in is the only one under way, and it still shows its stake.
            assertThat(weeks.get(3).state()).isEqualTo(ColonyWeekOutcomeState.CURRENT);
            assertThat(weeks.get(3).housingGain()).isEqualTo(350);
        });
    }

    /**
     * Verifies a week nobody ever drew a boss for is quoted at nothing rather than at a stake.
     *
     * <p>A run is ten weeks long, not ten fights long, which is what keeps runs comparable.
     */
    @Test
    void shouldQuoteAWeekWithNoFightAtNothing() {
        givenTheWorkedState();

        assertThat(service.findCurrent().weeks()).allSatisfy(week -> {
            assertThat(week.category()).isNull();
            assertThat(week.materials()).isZero();
            assertThat(week.moraleDelta()).isZero();
        });
    }

    /**
     * Verifies the turnout names every player of the roster, and separates the ones who played under the
     * threshold from the ones who did not play at all.
     */
    @Test
    void shouldSeparatePlayingUnderTheThresholdFromNotPlaying() {
        givenTheWorkedState();
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of(player(1L, "Thomas"), player(2L, "Rémi"), player(3L, "Yanis")));
        when(activityReader.readRawDamageByPlayer(TODAY)).thenReturn(Map.of(1L, 850, 2L, 200));

        assertThat(service.findCurrent().presence()).satisfies(presence -> {
            assertThat(presence.present()).isEqualTo(5);
            assertThat(presence.rosterSize()).isEqualTo(7);
            assertThat(presence.multiplier()).isEqualTo(1.714, within(1e-3));
            assertThat(presence.threshold()).isEqualTo(300);
            assertThat(presence.players()).extracting("state").containsExactly(
                ColonyPresenceState.FULL,
                ColonyPresenceState.PARTIAL,
                ColonyPresenceState.NONE
            );
        });
    }

    /**
     * Verifies the curve carries both ceilings alongside the population, which is what makes the days
     * they cross readable.
     */
    @Test
    void shouldDrawTheCurveWithBothCeilings() {
        givenTheWorkedState();

        assertThat(service.findTrajectory()).satisfies(trajectory -> {
            assertThat(trajectory.runNumber()).isEqualTo(3);
            assertThat(trajectory.points()).singleElement().satisfies(point -> {
                assertThat(point.runDay()).isEqualTo(26);
                assertThat(point.population()).isEqualTo(2_400);
                assertThat(point.feedablePopulation()).isEqualTo(3_520);
                assertThat(point.capacity()).isEqualTo(3_625);
                assertThat(point.morale()).isEqualTo(55.0, within(TOLERANCE));
            });
        });
    }

    /**
     * Verifies the curve marks the days the town changed name, and only those.
     */
    @Test
    void shouldMarkTheDaysTheTownChangedName() {
        givenSnapshots(
            snapshot(FIRST_WEEK, 440.0, 2_400.0, 92.0, 55.0, 1_800, 3_000),
            snapshot(FIRST_WEEK.plusDays(1), 440.0, 2_400.0, 92.0, 55.0, 1_900, 3_050),
            snapshot(FIRST_WEEK.plusDays(2), 440.0, 2_400.0, 92.0, 55.0, 3_050, 3_625)
        );

        assertThat(service.findTrajectory().milestones()).singleElement().satisfies(milestone -> {
            assertThat(milestone.name()).isEqualTo(ColonyTierName.BOROUGH);
            assertThat(milestone.runDay()).isEqualTo(3);
            assertThat(milestone.threshold()).isEqualTo(3_500);
        });
    }

    /**
     * Verifies a run with no snapshot at all is replayed once rather than failing.
     */
    @Test
    void shouldReplayARunThatHasNoSnapshotYet() {
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(RUN_ID))
            .thenReturn(List.of())
            .thenReturn(List.of(snapshot(TODAY, 440.0, 2_400.0, 92.0, 55.0, 3_050, 3_625)));

        assertThat(service.findCurrent().population()).isEqualTo(2_400);
        verify(replayService).replay(any());
    }

    /**
     * Verifies a closed run reports the tier it finished on, and that a run older than the colony
     * reports zeroes rather than failing.
     */
    @Test
    void shouldReportHowAClosedRunEnded() {
        Run closed = run();
        closed.setClosedAt(Instant.parse("2026-08-10T00:05:00Z"));
        when(runService.closedRuns()).thenReturn(List.of(closed));
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(RUN_ID))
            .thenReturn(List.of(snapshot(TODAY, 440.0, 2_400.0, 92.0, 55.0, 3_050, 3_625)));

        assertThat(service.findHistory()).singleElement().satisfies(entry -> {
            assertThat(entry.finalPopulation()).isEqualTo(2_400);
            assertThat(entry.capacity()).isEqualTo(3_625);
            assertThat(entry.tier().name()).isEqualTo(ColonyTierName.BOROUGH);
            assertThat(entry.bossCount()).isEqualTo(10);
        });

        when(snapshotRepository.findAllByRunIdOrderByDayAsc(RUN_ID)).thenReturn(List.of());

        assertThat(service.findHistory()).singleElement().satisfies(entry -> {
            assertThat(entry.finalPopulation()).isZero();
            assertThat(entry.peakPopulation()).isZero();
            assertThat(entry.averagePopulation()).isZero();
        });
    }

    /**
     * Registers the worked state of the interface document as the run's only day.
     */
    private void givenTheWorkedState() {
        givenSnapshots(snapshot(TODAY, 440.0, 2_400.0, 92.0, 55.0, 3_050, 3_625));
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
        run.setNumber(3);
        run.setFirstWeekStart(FIRST_WEEK);
        run.setLastWeekStart(FIRST_WEEK.plusWeeks(9));
        run.setRosterSize(7);

        return run;
    }

    /**
     * Builds one roster player fixture.
     *
     * @param id   player identifier
     * @param name display name
     * @return the player
     */
    private static Player player(long id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(name);

        return player;
    }

    /**
     * Builds a finalized fight fixture.
     *
     * @param weekStart Monday beginning the week
     * @param category  category the boss was drawn at
     * @param defeated  whether the boss went down
     * @return the encounter
     */
    private static WeeklyBossEncounter encounter(
        LocalDate weekStart,
        BossCategory category,
        boolean defeated
    ) {
        return encounter(weekStart, category, defeated, Instant.parse("2026-06-08T00:05:00Z"));
    }

    /**
     * Builds a fight fixture, settled or not.
     *
     * @param weekStart   Monday beginning the week
     * @param category    category the boss was drawn at
     * @param defeated    whether the boss went down
     * @param finalizedAt instant the week became immutable, {@code null} while it is still open
     * @return the encounter
     */
    private static WeeklyBossEncounter encounter(
        LocalDate weekStart,
        BossCategory category,
        boolean defeated,
        Instant finalizedAt
    ) {
        BossCatalogEntry boss = new BossCatalogEntry();
        boss.setCategory(category);

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(weekStart);
        encounter.setBossCatalogEntry(boss);
        encounter.setDefeated(defeated);
        encounter.setFinalizedAt(finalizedAt);

        return encounter;
    }

    /**
     * Builds one snapshot fixture.
     *
     * @param day              calendar day
     * @param foodStock        food of the last seven days
     * @param population       population at the end of the day
     * @param populationChange what the night moved
     * @param morale           morale the day ends on
     * @param materials        cumulative materials
     * @param capacity         housing available
     * @return the snapshot
     */
    private static ColonyDailySnapshot snapshot(
        LocalDate day,
        double foodStock,
        double population,
        double populationChange,
        double morale,
        int materials,
        int capacity
    ) {
        ColonyDailySnapshot snapshot = new ColonyDailySnapshot();
        snapshot.setDay(day);
        snapshot.setFoodStock(BigDecimal.valueOf(foodStock));
        snapshot.setFoodHarvest(BigDecimal.valueOf(92.0));
        snapshot.setMatchDamage(4_600);
        snapshot.setPresenceCount(5);
        snapshot.setMorale(BigDecimal.valueOf(morale));
        snapshot.setMaterials(materials);
        snapshot.setCapacity(capacity);
        snapshot.setPopulation(BigDecimal.valueOf(population));
        snapshot.setPopulationChange(BigDecimal.valueOf(populationChange));

        return snapshot;
    }
}
