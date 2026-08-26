package io.github.thomashtn.valoquests.colony.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands out the run in progress and the days it has lived.
 *
 * <p>Groups the one behaviour a reader must not get wrong: a page asking for the colony should never
 * depend on a scheduled job having fired. Both the run and its snapshots are therefore created on
 * demand, exactly as the boss endpoint already draws a week's fight lazily.
 */
@Service
public class ColonyRunReader {

    /**
     * Service resolving the run in progress.
     */
    private final RunService runService;

    /**
     * Service rebuilding a run that has no snapshot yet.
     */
    private final ColonyReplayService replayService;

    /**
     * Snapshot repository.
     */
    private final ColonyDailySnapshotRepository snapshotRepository;

    /**
     * Calendar resolving the current week and where a day falls in it.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the run reader.
     *
     * @param runService         run service
     * @param replayService      colony replay service
     * @param snapshotRepository colony daily snapshot repository
     * @param weekCalendar       week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public ColonyRunReader(
        RunService runService,
        ColonyReplayService replayService,
        ColonyDailySnapshotRepository snapshotRepository,
        WeekCalendar weekCalendar
    ) {
        this.runService = runService;
        this.replayService = replayService;
        this.snapshotRepository = snapshotRepository;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the run in progress, opening one when the deployment has not seen a rollover yet.
     *
     * @return the run in progress
     */
    @Transactional
    public Run currentRun() {
        return runService.ensureRunFor(weekCalendar.currentWeekStart());
    }

    /**
     * Returns every closed run, most recent first.
     *
     * @return closed runs
     */
    @Transactional(readOnly = true)
    public List<Run> closedRuns() {
        return runService.closedRuns();
    }

    /**
     * Returns a run's snapshots, replaying it once when it has none yet.
     *
     * @param run run to read
     * @return the run's snapshots, oldest day first
     */
    @Transactional
    public List<ColonyDailySnapshot> snapshotsOf(Run run) {
        List<ColonyDailySnapshot> snapshots =
            snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());

        if (!snapshots.isEmpty()) {
            return snapshots;
        }

        replayService.replay(run);

        return snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());
    }

    /**
     * Returns a closed run's snapshots without ever replaying it.
     *
     * <p>A closed run is frozen: its score never moves again, so a later rebalancing of the ruleset
     * cannot rewrite history. A run that ended before the colony existed simply has no days.
     *
     * @param run closed run to read
     * @return the run's snapshots, oldest day first, possibly empty
     */
    @Transactional(readOnly = true)
    public List<ColonyDailySnapshot> settledSnapshotsOf(Run run) {
        return snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());
    }

    /**
     * Places a calendar day inside its run, counting from one.
     *
     * @param run run the day belongs to
     * @param day day to place
     * @return the day's one-based position in the run
     */
    public int runDayOf(Run run, LocalDate day) {
        return (int) ChronoUnit.DAYS.between(run.getFirstWeekStart(), day) + 1;
    }

    /**
     * Places a calendar day's week inside its run, counting from one.
     *
     * @param run run the day belongs to
     * @param day day to place
     * @return the week's one-based position in the run
     */
    public int runWeekOf(Run run, LocalDate day) {
        return (int) ChronoUnit.WEEKS.between(
            run.getFirstWeekStart(),
            weekCalendar.weekStartOf(day)
        ) + 1;
    }

    /**
     * Returns the Monday of the week a day falls in.
     *
     * @param day day to place
     * @return Monday beginning that day's week
     */
    public LocalDate weekStartOf(LocalDate day) {
        return weekCalendar.weekStartOf(day);
    }
}
