package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayActivity;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests which days of a run carry a rollover's materials, and how far a replay reaches.
 */
class ColonyRunInputAssemblerTest {

    /** Monday the fixture run opens on. */
    private static final LocalDate FIRST_WEEK = LocalDate.of(2026, 6, 1);

    /** Materials the fixture materials reader returns for any finished week. */
    private static final int WEEKLY_MATERIALS = 847;

    /** Activity reader dependency. */
    private ColonyActivityReader activityReader;

    /** Materials reader dependency. */
    private ColonyMaterialsReader materialsReader;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        activityReader = mock(ColonyActivityReader.class);
        materialsReader = mock(ColonyMaterialsReader.class);

        lenient().when(activityReader.readActivity(any(), any())).thenReturn(Map.of());
        lenient().when(materialsReader.materialsOf(any())).thenReturn(WEEKLY_MATERIALS);
    }

    /**
     * Verifies that a finished run holds seventy-one days, from its first to its settlement day.
     */
    @Test
    void shouldAssembleSeventyOneDaysForAFinishedRun() {
        List<ColonyDailyInput> days = assemble(FIRST_WEEK.plusWeeks(20));

        assertThat(days).hasSize(71);
        assertThat(days.getFirst().day()).isEqualTo(FIRST_WEEK);
        assertThat(days.getLast().day()).isEqualTo(FIRST_WEEK.plusDays(70));
    }

    /**
     * Verifies that a run in progress stops at today, so the curve draws no day that has not happened.
     */
    @Test
    void shouldStopAtTodayForARunInProgress() {
        List<ColonyDailyInput> days = assemble(FIRST_WEEK.plusDays(37));

        assertThat(days).hasSize(38);
        assertThat(days.getLast().day()).isEqualTo(FIRST_WEEK.plusDays(37));
    }

    /**
     * Verifies that exactly ten days of a run credit materials: every Monday but the first.
     *
     * <p>Day eight credits week one, day fifteen credits week two, and so on to the settlement day,
     * which credits the tenth. Without that last one, the tenth week would be the only one to bring
     * nothing in.
     */
    @Test
    void shouldCreditMaterialsOnEveryMondayButTheFirst() {
        List<ColonyDailyInput> days = assemble(FIRST_WEEK.plusWeeks(20));

        List<ColonyDailyInput> creditedDays = days.stream()
            .filter(day -> day.creditedMaterials() > 0)
            .toList();

        assertThat(creditedDays).hasSize(10);
        assertThat(creditedDays).extracting(ColonyDailyInput::day).containsExactly(
            FIRST_WEEK.plusDays(7),
            FIRST_WEEK.plusDays(14),
            FIRST_WEEK.plusDays(21),
            FIRST_WEEK.plusDays(28),
            FIRST_WEEK.plusDays(35),
            FIRST_WEEK.plusDays(42),
            FIRST_WEEK.plusDays(49),
            FIRST_WEEK.plusDays(56),
            FIRST_WEEK.plusDays(63),
            FIRST_WEEK.plusDays(70)
        );
        assertThat(days.getFirst().creditedMaterials()).isZero();
    }

    /**
     * Verifies that each rollover credits the week that just ended, never the one it opens.
     */
    @Test
    void shouldCreditTheWeekThatJustEnded() {
        assemble(FIRST_WEEK.plusWeeks(20));

        verify(materialsReader).materialsOf(FIRST_WEEK);
        verify(materialsReader).materialsOf(FIRST_WEEK.plusWeeks(9));
        verify(materialsReader, never()).materialsOf(FIRST_WEEK.minusWeeks(1));
        verify(materialsReader, never()).materialsOf(FIRST_WEEK.plusWeeks(10));
    }

    /**
     * Verifies that a day's activity is carried through, and that a day nobody played is a zero rather
     * than a hole.
     */
    @Test
    void shouldCarryDailyActivityThroughAndTreatSilentDaysAsZero() {
        when(activityReader.readActivity(any(), any())).thenReturn(Map.of(
            FIRST_WEEK, new ColonyDayActivity(7_000, 7)
        ));

        List<ColonyDailyInput> days = assemble(FIRST_WEEK.plusDays(2));

        assertThat(days.getFirst().matchDamage()).isEqualTo(7_000);
        assertThat(days.getFirst().activePlayerCount()).isEqualTo(7);
        assertThat(days.get(1).matchDamage()).isZero();
        assertThat(days.get(1).activePlayerCount()).isZero();
    }

    /**
     * Verifies that a run whose first week has not started yet assembles nothing.
     */
    @Test
    void shouldAssembleNothingBeforeARunHasStarted() {
        assertThat(assemble(FIRST_WEEK.minusDays(1))).isEmpty();
    }

    /**
     * Assembles the fixture run as of a given day.
     *
     * @param today day the application clock is pinned to
     * @return the assembled days
     */
    private List<ColonyDailyInput> assemble(LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        WeekCalendar weekCalendar = new WeekCalendar(clock, ZoneOffset.UTC);

        ColonyRunInputAssembler assembler =
            new ColonyRunInputAssembler(activityReader, materialsReader, weekCalendar, clock);

        return assembler.assemble(run());
    }

    /**
     * Builds the fixture run.
     *
     * @return a ten-week run opening on {@link #FIRST_WEEK}
     */
    private static Run run() {
        Run run = new Run();
        run.setId(1L);
        run.setNumber(1);
        run.setFirstWeekStart(FIRST_WEEK);
        run.setLastWeekStart(FIRST_WEEK.plusWeeks(9));
        run.setRosterSize(7);

        return run;
    }
}
