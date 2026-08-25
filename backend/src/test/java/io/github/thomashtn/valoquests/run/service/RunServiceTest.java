package io.github.thomashtn.valoquests.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.repository.RunRepository;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the lifecycle of the ten-week runs the campaign is bounded by.
 */
class RunServiceTest {

    /** Monday the first fixture run opens on. */
    private static final LocalDate FIRST_WEEK = LocalDate.of(2026, 6, 1);

    /** Roster size every fixture run is frozen with. */
    private static final int ROSTER_SIZE = 7;

    /** Weeks a run spans, as the ruleset defines it. */
    private static final int RUN_LENGTH_WEEKS = 10;

    /** Run repository dependency, backed by {@link #runsByWeek}. */
    private RunRepository runRepository;

    /** Player repository dependency, supplying the roster size a run is frozen with. */
    private PlayerRepository playerRepository;

    /**
     * The run table, in memory, keyed by first week.
     *
     * <p>A fake rather than a bare mock: run creation now goes through an {@code ON CONFLICT DO
     * NOTHING} insert followed by a read, so stubbing the two calls independently would let a test
     * pass against a repository that never actually stored anything.
     */
    private Map<LocalDate, Run> runsByWeek;

    /** Service under test. */
    private RunService service;

    /** Creates the collaborators before each test. */
    @BeforeEach
    void setUp() {
        runRepository = mock(RunRepository.class);
        playerRepository = mock(PlayerRepository.class);
        runsByWeek = new LinkedHashMap<>();

        lenient().when(playerRepository.countByStatus(PlayerStatus.ACTIVE))
            .thenReturn((long) ROSTER_SIZE);

        lenient().when(runRepository.findByClosedAtIsNull()).thenAnswer(invocation ->
            runsByWeek.values().stream().filter(run -> run.getClosedAt() == null).findFirst());
        lenient().when(runRepository.findTopByOrderByNumberDesc()).thenAnswer(invocation ->
            runsByWeek.values().stream().max(Comparator.comparingInt(Run::getNumber)));
        lenient().when(runRepository.findByFirstWeekStart(any())).thenAnswer(invocation ->
            Optional.ofNullable(runsByWeek.get(invocation.getArgument(0, LocalDate.class))));
        lenient().when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        lenient().doAnswer(invocation -> {
            int number = invocation.getArgument(0);
            LocalDate firstWeekStart = invocation.getArgument(1);

            boolean taken = runsByWeek.containsKey(firstWeekStart)
                || runsByWeek.values().stream().anyMatch(run -> run.getNumber() == number);

            if (!taken) {
                runsByWeek.put(firstWeekStart, storedRun(
                    number,
                    firstWeekStart,
                    invocation.getArgument(2),
                    invocation.getArgument(3)
                ));
            }

            return null;
        }).when(runRepository).insertIfAbsent(anyInt(), any(), any(), anyInt());

        Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:05:00Z"), ZoneOffset.UTC);

        service = new RunService(
            runRepository,
            playerRepository,
            new DefaultColonyRuleset(new DefaultScoringRuleset()),
            new WeekCalendar(clock, ZoneOffset.UTC),
            clock
        );
    }

    /**
     * Verifies that the first rollover following a deployment opens run one on a clean base.
     */
    @Test
    void shouldOpenTheFirstRunWhenNoneExists() {
        Run run = service.ensureRunFor(FIRST_WEEK);

        assertThat(run.getNumber()).isEqualTo(1);
        assertThat(run.getFirstWeekStart()).isEqualTo(FIRST_WEEK);
        assertThat(run.getLastWeekStart()).isEqualTo(FIRST_WEEK.plusWeeks(RUN_LENGTH_WEEKS - 1L));
        assertThat(run.getClosedAt()).isNull();
    }

    /**
     * Verifies that the roster size is frozen on the run rather than read live.
     *
     * <p>It is the denominator of the colony's Energy gauge. Read live, archiving a player would
     * rewrite the history of a run that has already been played.
     */
    @Test
    void shouldFreezeTheRosterSizeWhenOpeningARun() {
        Run run = service.ensureRunFor(FIRST_WEEK);

        assertThat(run.getRosterSize()).isEqualTo(ROSTER_SIZE);

        when(playerRepository.countByStatus(PlayerStatus.ACTIVE)).thenReturn(4L);

        assertThat(run.getRosterSize()).isEqualTo(ROSTER_SIZE);
    }

    /**
     * Verifies that a run spans ten weeks plus a settlement day, whatever happens inside it.
     *
     * <p>Counting rollovers rather than boss encounters is what guarantees this: a week can go by with
     * no encounter drawn, and counting encounters would make a run's length variable — exactly the
     * defect of the act it replaces.
     */
    @Test
    void shouldSpanTenWeeksAndASettlementDay() {
        Run run = service.ensureRunFor(FIRST_WEEK);

        assertThat(run.settlementDay()).isEqualTo(FIRST_WEEK.plusWeeks(RUN_LENGTH_WEEKS));
        assertThat(run.settlementDay()).isEqualTo(FIRST_WEEK.plusDays(70));
        assertThat(run.covers(FIRST_WEEK)).isTrue();
        assertThat(run.covers(run.settlementDay())).isTrue();
        assertThat(run.covers(run.settlementDay().plusDays(1))).isFalse();
        assertThat(run.covers(FIRST_WEEK.minusDays(1))).isFalse();
    }

    /**
     * Verifies that a week the open run already covers returns it untouched.
     */
    @Test
    void shouldBeIdempotentForAWeekTheOpenRunAlreadyCovers() {
        Run existing = givenOpenRun(1, FIRST_WEEK);

        Run resolved = service.ensureRunFor(FIRST_WEEK.plusWeeks(5));

        assertThat(resolved).isSameAs(existing);
        verify(runRepository, never()).insertIfAbsent(anyInt(), any(), any(), anyInt());
    }

    /**
     * Verifies that the eleventh week closes the run and opens the next one on the Monday right after.
     *
     * <p>Contiguity is what the settlement day depends on: a run's seventy-first day is also the first
     * day of its successor, and the two coexist because snapshots are indexed by run and by day.
     */
    @Test
    void shouldCloseTheRunAndOpenTheNextOneContiguously() {
        Run first = givenOpenRun(1, FIRST_WEEK);

        Run second = service.ensureRunFor(first.settlementDay());

        assertThat(first.getClosedAt()).isNotNull();
        assertThat(second.getNumber()).isEqualTo(2);
        assertThat(second.getFirstWeekStart()).isEqualTo(first.settlementDay());
        assertThat(second.getClosedAt()).isNull();
    }

    /**
     * Verifies that several expired runs are caught up in one pass.
     *
     * <p>{@code DefaultWeeklyRolloverService} finalizes every week it missed and then opens the current
     * one once, so a rollover firing after a long outage can be several runs behind. A single
     * transition would leave a gap in the numbering and orphan the weeks in between.
     */
    @Test
    void shouldCatchUpSeveralRunsAfterALongOutage() {
        givenOpenRun(1, FIRST_WEEK);

        // Twenty-five weeks in: run one is long over, run two too, and run three is the one running.
        Run resolved = service.ensureRunFor(FIRST_WEEK.plusWeeks(25));

        assertThat(resolved.getNumber()).isEqualTo(3);
        assertThat(resolved.getFirstWeekStart()).isEqualTo(FIRST_WEEK.plusWeeks(20));
        assertThat(resolved.covers(FIRST_WEEK.plusWeeks(25))).isTrue();
        assertThat(runsByWeek.values()).extracting(Run::getNumber).containsExactly(1, 2, 3);
    }

    /**
     * Verifies that a database whose only run is closed opens the next number, not one again.
     *
     * <p>Runs are normally contiguous, so nothing open means nothing was ever opened. A run closed on
     * its own breaks that assumption, and assuming run one would collide with the unique numbering
     * and fail every read of the colony from then on.
     */
    @Test
    void shouldOpenTheNextNumberWhenEveryRunIsClosed() {
        Run first = givenOpenRun(1, FIRST_WEEK);
        first.setClosedAt(Instant.parse("2026-08-10T00:05:00Z"));

        Run reopened = service.ensureRunFor(FIRST_WEEK.plusWeeks(20));

        assertThat(reopened.getNumber()).isEqualTo(2);
        assertThat(reopened.getFirstWeekStart()).isEqualTo(FIRST_WEEK.plusWeeks(20));
    }

    /**
     * Verifies that losing the race to open a run returns the winner's row rather than failing.
     *
     * <p>Several endpoints open a run lazily and the colony page fires them in parallel, so on the
     * first load two requests read "no run yet" from the same snapshot and both go on to insert. The
     * insert is written to no-op on conflict, so the loser has to read back what the winner wrote.
     */
    @Test
    void shouldReturnTheExistingRunWhenAConcurrentRequestOpenedItFirst() {
        // The winner's row, already committed, with the reader still holding a snapshot without it.
        runsByWeek.put(FIRST_WEEK, storedRun(1, FIRST_WEEK, FIRST_WEEK.plusWeeks(9), ROSTER_SIZE));
        when(runRepository.findByClosedAtIsNull()).thenReturn(Optional.empty());

        Run resolved = service.ensureRunFor(FIRST_WEEK);

        assertThat(resolved.getNumber()).isEqualTo(1);
        assertThat(resolved.getFirstWeekStart()).isEqualTo(FIRST_WEEK);
        assertThat(runsByWeek).hasSize(1);
    }

    /**
     * Verifies that a run can only ever be resolved from a Monday.
     */
    @Test
    void shouldRejectAWeekStartThatIsNotAMonday() {
        assertThatThrownBy(() -> service.ensureRunFor(FIRST_WEEK.plusDays(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Monday");
    }

    /**
     * Verifies that the run in progress is the only one left open.
     */
    @Test
    void shouldExposeTheOpenRunAndItsIdentifier() {
        Run open = givenOpenRun(4, FIRST_WEEK);

        assertThat(service.currentRun()).contains(open);
        assertThat(service.currentRunId()).contains(open.getId());
    }

    /**
     * Verifies that no run is reported before the first rollover following a deployment.
     */
    @Test
    void shouldReportNoRunBeforeTheFirstRollover() {
        assertThat(service.currentRun()).isEmpty();
        assertThat(service.currentRunId()).isEmpty();
    }

    /**
     * Seeds the run table with one open run.
     *
     * @param number    sequential run number
     * @param weekStart Monday the run's first week starts on
     * @return the seeded run
     */
    private Run givenOpenRun(int number, LocalDate weekStart) {
        Run run = storedRun(
            number,
            weekStart,
            weekStart.plusWeeks(RUN_LENGTH_WEEKS - 1L),
            ROSTER_SIZE
        );
        runsByWeek.put(weekStart, run);

        return run;
    }

    /**
     * Builds one row of the in-memory run table.
     *
     * @param number         sequential run number
     * @param firstWeekStart Monday the run's first week starts on
     * @param lastWeekStart  Monday the run's tenth week starts on
     * @param rosterSize     roster size frozen on the run
     * @return the run
     */
    private static Run storedRun(
        int number,
        LocalDate firstWeekStart,
        LocalDate lastWeekStart,
        int rosterSize
    ) {
        Run run = new Run();
        run.setId((long) number);
        run.setNumber(number);
        run.setFirstWeekStart(firstWeekStart);
        run.setLastWeekStart(lastWeekStart);
        run.setRosterSize(rosterSize);

        return run;
    }
}
