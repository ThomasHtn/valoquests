package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayState;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests that a replay rewrites a run rather than advancing it.
 */
class ColonyReplayServiceTest {

    /** Monday the fixture run opens on. */
    private static final LocalDate FIRST_WEEK = LocalDate.of(2026, 6, 1);

    /** Roster the fixture run is frozen with. */
    private static final int ROSTER_SIZE = 7;

    /** Scale the NUMERIC columns keep, which is display precision rather than the engine's. */
    private static final Offset<Double> STORED = within(0.001);

    /** Run service dependency. */
    private RunService runService;

    /** Input assembler dependency. */
    private ColonyRunInputAssembler inputAssembler;

    /** Snapshot repository dependency. */
    private ColonyDailySnapshotRepository snapshotRepository;

    /** Service under test, wired with the real engine. */
    private ColonyReplayService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        runService = mock(RunService.class);
        inputAssembler = mock(ColonyRunInputAssembler.class);
        snapshotRepository = mock(ColonyDailySnapshotRepository.class);

        lenient().when(snapshotRepository.saveAll(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service = new ColonyReplayService(
            runService,
            inputAssembler,
            new ColonyReplayEngine(new DefaultColonyRuleset(new DefaultScoringRuleset())),
            snapshotRepository
        );
    }

    /**
     * Verifies that nothing is touched before a run has been opened.
     */
    @Test
    void shouldDoNothingWhileNoRunIsInProgress() {
        when(runService.currentRun()).thenReturn(Optional.empty());

        assertThat(service.replayCurrentRun()).isEmpty();
        verifyNoInteractions(snapshotRepository, inputAssembler);
    }

    /**
     * Verifies that a replay deletes the run's rows before writing them again.
     *
     * <p>Deleted rather than updated in place, so a replay cannot leave a day behind that its inputs no
     * longer justify.
     */
    @Test
    void shouldRewriteRatherThanAdvance() {
        Run run = givenRunWithDays(5);

        service.replayCurrentRun();

        verify(snapshotRepository).deleteAllByRunId(run.getId());
        verify(snapshotRepository).saveAll(any());
    }

    /**
     * Verifies that two consecutive replays produce strictly identical rows.
     *
     * <p>The property that makes the nightly tick, the post-synchronization hook and the admin
     * recompute safe to run in any order and any number of times.
     */
    @Test
    void shouldBeIdempotentAcrossConsecutiveReplays() {
        givenRunWithDays(30);

        service.replayCurrentRun();
        service.replayCurrentRun();

        ArgumentCaptor<List<ColonyDailySnapshot>> captor = ArgumentCaptor.captor();
        verify(snapshotRepository, times(2)).saveAll(captor.capture());

        List<ColonyDailySnapshot> first = captor.getAllValues().get(0);
        List<ColonyDailySnapshot> second = captor.getAllValues().get(1);

        assertThat(second).hasSameSizeAs(first);
        for (int index = 0; index < first.size(); index++) {
            assertSameSnapshot(first.get(index), second.get(index));
        }
    }

    /**
     * Verifies that a replay covering days no previous one reached rebuilds every one of them.
     *
     * <p>Three days of downtime are caught up by the next replay, with no manual repair: the engine
     * starts from the run's first day regardless of what was last written.
     */
    @Test
    void shouldRebuildDaysAPreviousReplayNeverReached() {
        givenRunWithDays(7);
        service.replayCurrentRun();

        givenRunWithDays(10);
        service.replayCurrentRun();

        ArgumentCaptor<List<ColonyDailySnapshot>> captor = ArgumentCaptor.captor();
        verify(snapshotRepository, times(2)).saveAll(captor.capture());

        assertThat(captor.getAllValues().get(0)).hasSize(7);
        assertThat(captor.getAllValues().get(1)).hasSize(10);
        assertSameSnapshot(
            captor.getAllValues().get(0).get(6),
            captor.getAllValues().get(1).get(6)
        );
    }

    /**
     * Verifies that every computed field lands on the row that stores it.
     */
    @Test
    void shouldMapEveryComputedFieldOntoItsRow() {
        Run run = givenRunWithDays(1);

        List<ColonyDayState> states = service.replayCurrentRun();

        ArgumentCaptor<List<ColonyDailySnapshot>> captor = ArgumentCaptor.captor();
        verify(snapshotRepository).saveAll(captor.capture());
        ColonyDailySnapshot snapshot = captor.getValue().getFirst();
        ColonyDayState state = states.getFirst();

        assertThat(snapshot.getRun()).isSameAs(run);
        assertThat(snapshot.getDay()).isEqualTo(state.day());
        // Compared at the scale the column keeps: the row is display precision, the state is not.
        assertThat(snapshot.getFoodStock().doubleValue()).isEqualTo(state.foodStock(), STORED);
        assertThat(snapshot.getFoodHarvest().doubleValue()).isEqualTo(state.foodHarvest(), STORED);
        assertThat(snapshot.getMatchDamage()).isEqualTo(state.matchDamage());
        assertThat(snapshot.getPresenceCount()).isEqualTo(state.presencePlayerCount());
        assertThat(snapshot.getMorale().doubleValue()).isEqualTo(state.morale());
        assertThat(snapshot.getMaterials()).isEqualTo(state.materials());
        assertThat(snapshot.getCapacity()).isEqualTo(state.capacity());
        assertThat(snapshot.getPopulation().doubleValue()).isEqualTo(state.population(), STORED);
        assertThat(snapshot.getPopulationChange().doubleValue())
            .isEqualTo(state.populationChange(), STORED);
    }

    /**
     * Verifies that a run whose first day has not come yet writes nothing but still clears its rows.
     */
    @Test
    void shouldWriteNothingForARunWithNoDayYet() {
        givenRunWithDays(0);

        assertThat(service.replayCurrentRun()).isEmpty();

        ArgumentCaptor<List<ColonyDailySnapshot>> captor = ArgumentCaptor.captor();
        verify(snapshotRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    /**
     * Verifies that a run closed short of its settlement day is replayed one last time.
     *
     * <p>The rollover closes a run on the very Monday that is its seventy-first day, and only the open
     * run is replayed — so that day, which credits the tenth week's challenges and boss, was never
     * computed for anybody.
     */
    @Test
    void shouldSettleAClosedRunThatNeverReachedItsSettlementDay() {
        Run closedRun = givenClosedRunLastReplayedOn(FIRST_WEEK.plusDays(69));
        givenRunWithDays(1);

        service.replayCurrentRun();

        verify(snapshotRepository).deleteAllByRunId(closedRun.getId());
        verify(inputAssembler).assemble(closedRun);
    }

    /**
     * Verifies that a run already settled is left strictly alone.
     *
     * <p>What keeps a closed run frozen: a later rebalancing of the ruleset must never rewrite a score
     * that has already been carried.
     */
    @Test
    void shouldLeaveAnAlreadySettledRunUntouched() {
        Run closedRun = givenClosedRunLastReplayedOn(FIRST_WEEK.plusDays(70));
        givenRunWithDays(1);

        service.replayCurrentRun();

        verify(snapshotRepository, times(0)).deleteAllByRunId(closedRun.getId());
        verify(inputAssembler, times(0)).assemble(closedRun);
    }

    /**
     * Registers one closed run and the last day its snapshots reach.
     *
     * @param lastDay last day the run has a snapshot for
     * @return the closed run
     */
    private Run givenClosedRunLastReplayedOn(LocalDate lastDay) {
        Run run = new Run();
        run.setId(9L);
        run.setNumber(9);
        run.setFirstWeekStart(FIRST_WEEK);
        run.setLastWeekStart(FIRST_WEEK.plusWeeks(9));
        run.setRosterSize(ROSTER_SIZE);
        run.setClosedAt(Instant.EPOCH);

        when(runService.closedRuns()).thenReturn(List.of(run));
        when(snapshotRepository.findLastDayByRunId(run.getId())).thenReturn(Optional.of(lastDay));
        lenient().when(inputAssembler.assemble(run)).thenReturn(List.of());

        return run;
    }

    /**
     * Registers a run in progress and the days its replay will walk.
     *
     * @param dayCount number of days assembled
     * @return the run in progress
     */
    private Run givenRunWithDays(int dayCount) {
        Run run = new Run();
        run.setId(1L);
        run.setNumber(1);
        run.setFirstWeekStart(FIRST_WEEK);
        run.setLastWeekStart(FIRST_WEEK.plusWeeks(9));
        run.setRosterSize(ROSTER_SIZE);

        List<ColonyDailyInput> days = new ArrayList<>(dayCount);
        for (int index = 0; index < dayCount; index++) {
            boolean rollover = index % 7 == 0 && index > 0;

            days.add(new ColonyDailyInput(
                FIRST_WEEK.plusDays(index),
                index % 3 == 0 ? 7_000 : 2_000,
                index % 3 == 0 ? 7 : 3,
                rollover,
                rollover ? 847 : 0,
                rollover ? 15.0 : 0.0
            ));
        }

        when(runService.currentRun()).thenReturn(Optional.of(run));
        when(inputAssembler.assemble(run)).thenReturn(days);

        return run;
    }

    /**
     * Asserts that two rows describe exactly the same day.
     *
     * @param expected first row
     * @param actual   second row
     */
    private static void assertSameSnapshot(
        ColonyDailySnapshot expected,
        ColonyDailySnapshot actual
    ) {
        assertThat(actual.getDay()).isEqualTo(expected.getDay());
        assertThat(actual.getFoodStock()).isEqualByComparingTo(expected.getFoodStock());
        assertThat(actual.getMorale()).isEqualByComparingTo(expected.getMorale());
        assertThat(actual.getPopulation()).isEqualByComparingTo(expected.getPopulation());
        assertThat(actual.getMaterials()).isEqualTo(expected.getMaterials());
        assertThat(actual.getCapacity()).isEqualTo(expected.getCapacity());
    }
}
