package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayActivity;
import io.github.thomashtn.valoquests.colony.model.ColonyWeekOutcome;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a run into the list of days the replay engine consumes.
 *
 * <p>The only place the two readers are stitched together, and the only place the calendar decides which
 * day carries a rollover's materials.
 */
@Service
@Transactional(readOnly = true)
public class ColonyRunInputAssembler {

    /**
     * Reader supplying each day's match damage and turnout.
     */
    private final ColonyActivityReader activityReader;

    /**
     * Reader supplying each finished week's materials.
     */
    private final ColonyMaterialsReader materialsReader;

    /**
     * Calendar deciding where a week starts.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Application clock, deciding how far into the run the replay goes.
     */
    private final Clock clock;

    /**
     * Creates the input assembler.
     *
     * @param activityReader  activity reader
     * @param materialsReader materials reader
     * @param weekCalendar    week calendar
     * @param clock           application clock
     */
    public ColonyRunInputAssembler(
        ColonyActivityReader activityReader,
        ColonyMaterialsReader materialsReader,
        WeekCalendar weekCalendar,
        Clock clock
    ) {
        this.activityReader = activityReader;
        this.materialsReader = materialsReader;
        this.weekCalendar = weekCalendar;
        this.clock = clock;
    }

    /**
     * Assembles one run's days, from its first to today, never past its final day.
     *
     * <p>A closed run is assembled in full; the run in progress stops at today, so the curve does not
     * draw days that have not happened.
     *
     * @param run run to assemble, must not be {@code null}
     * @return the run's days in chronological order, empty when the run has not started yet
     */
    public List<ColonyDailyInput> assemble(Run run) {
        LocalDate firstDay = run.getFirstWeekStart();
        LocalDate lastDay = lastDayToReplay(run);

        if (lastDay.isBefore(firstDay)) {
            return List.of();
        }

        Map<LocalDate, ColonyDayActivity> activityByDay =
            activityReader.readActivity(firstDay, lastDay);

        List<ColonyDailyInput> days = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            ColonyDayActivity activity = activityByDay.getOrDefault(day, ColonyDayActivity.IDLE);
            boolean rollover = isRollover(run, day);
            ColonyWeekOutcome outcome = rollover
                ? materialsReader.outcomeOf(day.minusWeeks(1), run.getRosterSize())
                : ColonyWeekOutcome.NONE;

            days.add(new ColonyDailyInput(
                day,
                activity.matchDamage(),
                activity.presencePlayerCount(),
                rollover,
                outcome.materials(),
                outcome.moraleDelta()
            ));
        }

        return days;
    }

    /**
     * Returns the last day of the run the replay should reach.
     *
     * @param run run being replayed
     * @return today, or the run's final day once it is over
     */
    private LocalDate lastDayToReplay(Run run) {
        LocalDate today = LocalDate.now(clock.withZone(weekCalendar.zone()));

        return today.isAfter(run.finalDay()) ? run.finalDay() : today;
    }

    /**
     * Returns whether a day settles the week that has just closed.
     *
     * <p>True on exactly ten days: every Monday of the run except its first, which settles nothing
     * because no week of this run has closed yet. The last of them is the settlement day, the run's
     * seventy-first, which is what stops the tenth week from being the only one to bring nothing in.
     *
     * @param run run being replayed
     * @param day day being assembled
     * @return whether the day carries a rollover
     */
    private boolean isRollover(Run run, LocalDate day) {
        return weekCalendar.isWeekStart(day) && !day.equals(run.getFirstWeekStart());
    }
}
