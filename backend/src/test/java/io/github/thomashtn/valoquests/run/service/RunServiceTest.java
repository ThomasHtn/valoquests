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
import io.github.thomashtn.valoquests.run.entity.CampaignSettings;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.repository.CampaignSettingsRepository;
import io.github.thomashtn.valoquests.run.repository.RunRepository;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
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

    /** Run repository dependency, backed by {@link #runsByNumber}. */
    private RunRepository runRepository;

    /** Player repository dependency, supplying the roster size a run is frozen with. */
    private PlayerRepository playerRepository;

    /**
     * Campaign settings repository dependency, backed by a single fake row so
     * {@link RunService#isAutoRenewEnabled()} reads back whatever
     * {@link RunService#setAutoRenewEnabled(boolean)} last wrote.
     */
    private CampaignSettingsRepository campaignSettingsRepository;

    /**
     * The run table, in memory, keyed by run number.
     *
     * <p>A fake rather than a bare mock: run creation now goes through an {@code ON CONFLICT DO
     * NOTHING} insert followed by a read, so stubbing the two calls independently would let a test
     * pass against a repository that never actually stored anything.
     *
     * <p>Keyed by number rather than by first week, as the table itself is since {@code V37}: a run
     * an operator stopped and the one opened in its place share the Monday of the stop.
     */
    private Map<Integer, Run> runsByNumber;

    /** Service under test. */
    private RunService service;

    /** Creates the collaborators before each test. */
    @BeforeEach
    void setUp() {
        runRepository = mock(RunRepository.class);
        playerRepository = mock(PlayerRepository.class);
        campaignSettingsRepository = mock(CampaignSettingsRepository.class);
        runsByNumber = new LinkedHashMap<>();

        CampaignSettings[] settings = { new CampaignSettings() };
        lenient().when(campaignSettingsRepository.findById(CampaignSettings.SINGLETON_ID))
            .thenAnswer(invocation -> Optional.of(settings[0]));
        lenient().when(campaignSettingsRepository.save(any())).thenAnswer(invocation -> {
            settings[0] = invocation.getArgument(0);
            return settings[0];
        });

        lenient().when(playerRepository.countByStatus(PlayerStatus.ACTIVE))
            .thenReturn((long) ROSTER_SIZE);

        lenient().when(runRepository.findByClosedAtIsNull()).thenAnswer(invocation -> openRun());
        lenient().when(runRepository.findTopByOrderByNumberDesc()).thenAnswer(invocation ->
            runsByNumber.values().stream().max(Comparator.comparingInt(Run::getNumber)));
        lenient().when(runRepository.findById(any())).thenAnswer(invocation ->
            runsByNumber.values().stream()
                .filter(run -> invocation.getArgument(0).equals(run.getId()))
                .findFirst());
        lenient().when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doAnswer(invocation -> runsByNumber.values()
            .remove(invocation.getArgument(0, Run.class)))
            .when(runRepository).delete(any());

        // The two unique constraints the insert can conflict on: the run number, and at most one run
        // left open. Both make the insert a no-op rather than a failure.
        lenient().doAnswer(invocation -> {
            int number = invocation.getArgument(0);

            if (!runsByNumber.containsKey(number) && openRun().isEmpty()) {
                runsByNumber.put(number, storedRun(
                    number,
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3)
                ));
            }

            return null;
        }).when(runRepository).insertIfAbsent(anyInt(), any(), any(), anyInt());

        Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:05:00Z"), ZoneOffset.UTC);

        service = new RunService(
            runRepository,
            campaignSettingsRepository,
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
     * Verifies that an empty roster never freezes a run at zero.
     *
     * <p>A run opens lazily on the first page view, so a deployment whose roster has not been filled
     * in yet would freeze it at zero for ten weeks — and every per-player figure is multiplied by it,
     * so a defeated boss would pay nothing and no amount of play could move the run.
     */
    @Test
    void shouldNeverFreezeARunOnAnEmptyRoster() {
        when(playerRepository.countByStatus(PlayerStatus.ACTIVE)).thenReturn(0L);

        Run run = service.ensureRunFor(FIRST_WEEK);

        assertThat(run.getRosterSize()).isEqualTo(1);
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
        assertThat(runsByNumber.values()).extracting(Run::getNumber).containsExactly(1, 2, 3);
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
        // The winner's row, already committed. The loser still holds the snapshot it read "no run"
        // from, so only its first read misses it — the read back after the insert sees it.
        runsByNumber.put(1, storedRun(1, FIRST_WEEK, FIRST_WEEK.plusWeeks(9), ROSTER_SIZE));
        boolean[] stale = { true };
        when(runRepository.findByClosedAtIsNull()).thenAnswer(invocation -> {
            if (stale[0]) {
                stale[0] = false;
                return Optional.empty();
            }

            return openRun();
        });

        Run resolved = service.ensureRunFor(FIRST_WEEK);

        assertThat(resolved.getNumber()).isEqualTo(1);
        assertThat(resolved.getFirstWeekStart()).isEqualTo(FIRST_WEEK);
        assertThat(runsByNumber).hasSize(1);
    }

    /**
     * Verifies that a campaign stopped today can be replaced by a clean one on the same Monday.
     *
     * <p>The whole point of stopping one early. The run's first week used to be unique, so the
     * insert opening the replacement conflicted, did nothing — it is written {@code ON CONFLICT DO
     * NOTHING} — and the read that followed handed back the run that had just been stopped: the
     * campaign reported itself as started while nothing was open at all.
     */
    @Test
    void shouldStartACleanCampaignOnTheWeekTheStoppedOneWasCutOn() {
        givenOpenRun(1, FIRST_WEEK);
        Run stopped = service.stopCurrentRun();

        Run started = service.startRunNow();

        assertThat(started.getNumber()).isEqualTo(2);
        assertThat(started.getId()).isNotEqualTo(stopped.getId());
        assertThat(started.getFirstWeekStart()).isEqualTo(stopped.getFirstWeekStart());
        assertThat(started.getStoppedOn()).isNull();
        assertThat(started.getClosedAt()).isNull();
        assertThat(service.currentRun()).contains(started);
    }

    /**
     * Verifies that the lazy path, too, opens a clean run rather than resurrecting the stopped one.
     *
     * <p>Every colony and boss read goes through {@code ensureRunFor}, so this is the path that runs
     * first in practice — before an operator has had the chance to press anything.
     */
    @Test
    void shouldOpenACleanRunLazilyAfterACampaignWasStopped() {
        givenOpenRun(1, FIRST_WEEK);
        service.stopCurrentRun();

        Run resolved = service.ensureRunFor(FIRST_WEEK);

        assertThat(resolved.getNumber()).isEqualTo(2);
        assertThat(resolved.getClosedAt()).isNull();
        assertThat(runsByNumber).hasSize(2);
    }

    /**
     * Verifies that a run is deleted outright, and that its number is free again afterwards.
     */
    @Test
    void shouldDeleteARunAndFreeItsNumber() {
        Run open = givenOpenRun(1, FIRST_WEEK);

        service.deleteRun(open);

        assertThat(runsByNumber).isEmpty();
        assertThat(service.currentRun()).isEmpty();
        assertThat(service.startRunNow().getNumber()).isEqualTo(1);
    }

    /**
     * Verifies that deleting a campaign that does not exist is refused rather than silently a no-op.
     */
    @Test
    void shouldRefuseToReadARunThatDoesNotExist() {
        assertThatThrownBy(() -> service.findRun(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Campaign 404 does not exist.");
    }

    /**
     * Verifies that a run is read back by its own identifier.
     */
    @Test
    void shouldReadARunByItsIdentifier() {
        Run open = givenOpenRun(1, FIRST_WEEK);

        assertThat(service.findRun(open.getId())).isSameAs(open);
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
     * Verifies that automatic renewal reads back on, matching the setting's own default.
     */
    @Test
    void shouldDefaultAutoRenewToEnabled() {
        assertThat(service.isAutoRenewEnabled()).isTrue();
    }

    /**
     * Verifies that switching automatic renewal off reads back off.
     */
    @Test
    void shouldPersistAutoRenewOnceSwitched() {
        service.setAutoRenewEnabled(false);

        assertThat(service.isAutoRenewEnabled()).isFalse();
    }

    /**
     * Verifies that stopping the run in progress closes it, marks the day it stopped on, and leaves
     * its settlement day untouched — {@link Run#finalDay()} is what a stopped run's readers use
     * instead.
     */
    @Test
    void shouldStopTheRunInProgressOnToday() {
        Run open = givenOpenRun(1, FIRST_WEEK);
        LocalDate today = LocalDate.of(2026, 6, 1);

        Run stopped = service.stopCurrentRun();

        assertThat(stopped).isSameAs(open);
        assertThat(stopped.getStoppedOn()).isEqualTo(today);
        assertThat(stopped.getClosedAt()).isNotNull();
        assertThat(stopped.finalDay()).isEqualTo(today);
        assertThat(stopped.settlementDay()).isNotEqualTo(today);
    }

    /**
     * Verifies that stopping a campaign with none running is refused rather than silently a no-op.
     */
    @Test
    void shouldRefuseToStopWhenNoCampaignIsRunning() {
        assertThatThrownBy(() -> service.stopCurrentRun())
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("No campaign is currently running.");
    }

    /**
     * Verifies that starting a campaign opens a run on this week's Monday.
     */
    @Test
    void shouldStartACampaignOnThisWeeksMonday() {
        Run started = service.startRunNow();

        assertThat(started.getNumber()).isEqualTo(1);
        assertThat(started.getFirstWeekStart()).isEqualTo(FIRST_WEEK);
        assertThat(started.getClosedAt()).isNull();
    }

    /**
     * Verifies that starting a campaign while one is already running is refused.
     */
    @Test
    void shouldRefuseToStartWhenACampaignIsAlreadyRunning() {
        givenOpenRun(1, FIRST_WEEK);

        assertThatThrownBy(() -> service.startRunNow())
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("A campaign is already running.");
    }

    /**
     * Reads the in-memory table's open run, as the unique partial index guarantees there is at most
     * one of.
     *
     * @return the run left open, or empty when every run is closed
     */
    private Optional<Run> openRun() {
        return runsByNumber.values().stream().filter(run -> run.getClosedAt() == null).findFirst();
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
        runsByNumber.put(number, run);

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
