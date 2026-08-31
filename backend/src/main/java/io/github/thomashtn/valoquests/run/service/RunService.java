package io.github.thomashtn.valoquests.run.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.run.entity.CampaignSettings;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.repository.CampaignSettingsRepository;
import io.github.thomashtn.valoquests.run.repository.RunRepository;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the run the campaign is bounded by: which one is in progress, and when one gives way to the next.
 *
 * <p>Replaces {@code CampaignSeasonResolver}. A run belongs to no one — it bounds the campaign, and the
 * colony is only one of its consumers — so both the read and the write side of it live here rather than
 * inside either feature.
 */
@Service
public class RunService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RunService.class);

    /**
     * Number of the very first run.
     */
    private static final int FIRST_RUN_NUMBER = 1;

    /**
     * Smallest roster size a run may be frozen with.
     *
     * <p>Zero is reachable, and it is a trap: a run opens lazily on the first page view, so a
     * deployment whose roster has not been filled in yet freezes that run at zero for ten weeks.
     * Every per-player figure is then multiplied by it — a defeated boss pays {@code 0} materials,
     * efficiency never leaves its base, and no amount of play can move the run. Flooring it at one
     * costs a single-player deployment nothing it did not already have and turns a dead run into a
     * playable one.
     */
    private static final int MINIMUM_ROSTER_SIZE = 1;

    /**
     * Run repository.
     */
    private final RunRepository runRepository;

    /**
     * Repository backing the campaign's own lifecycle settings.
     */
    private final CampaignSettingsRepository campaignSettingsRepository;

    /**
     * Repository used to freeze the roster size a run opens on.
     */
    private final PlayerRepository playerRepository;

    /**
     * Ruleset supplying how many weeks a run spans.
     */
    private final ColonyRuleset ruleset;

    /**
     * Calendar validating week identifiers.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Application clock, used to stamp a run's closure.
     */
    private final Clock clock;

    /**
     * Creates the run service.
     *
     * @param runRepository              run repository
     * @param campaignSettingsRepository campaign settings repository
     * @param playerRepository           player repository
     * @param ruleset                    colony ruleset supplying the run length
     * @param weekCalendar               calendar validating week identifiers
     * @param clock                      application clock
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public RunService(
        RunRepository runRepository,
        CampaignSettingsRepository campaignSettingsRepository,
        PlayerRepository playerRepository,
        ColonyRuleset ruleset,
        WeekCalendar weekCalendar,
        Clock clock
    ) {
        this.runRepository = runRepository;
        this.campaignSettingsRepository = campaignSettingsRepository;
        this.playerRepository = playerRepository;
        this.ruleset = ruleset;
        this.weekCalendar = weekCalendar;
        this.clock = clock;
    }

    /**
     * Returns the run in progress.
     *
     * @return the open run, or empty before the first rollover following the deployment
     */
    @Transactional(readOnly = true)
    public Optional<Run> currentRun() {
        return runRepository.findByClosedAtIsNull();
    }

    /**
     * Returns the identifier of the run in progress.
     *
     * @return the open run's identifier, or empty while no run was ever opened
     */
    @Transactional(readOnly = true)
    public Optional<Long> currentRunId() {
        return currentRun().map(Run::getId);
    }

    /**
     * Returns every closed run, most recent first.
     *
     * @return closed runs ordered from the latest to the first
     */
    @Transactional(readOnly = true)
    public List<Run> closedRuns() {
        return runRepository.findAllByClosedAtIsNotNullOrderByNumberDesc();
    }

    /**
     * Returns whether the weekly rollover may open a new run once the current one closes.
     *
     * @return {@code true} unless an operator has turned automatic renewal off
     */
    @Transactional(readOnly = true)
    public boolean isAutoRenewEnabled() {
        return settings().isAutoRenewEnabled();
    }

    /**
     * Turns automatic renewal on or off.
     *
     * @param enabled whether the weekly rollover may open a new run on its own
     */
    @Transactional
    public void setAutoRenewEnabled(boolean enabled) {
        CampaignSettings settings = settings();
        settings.setAutoRenewEnabled(enabled);
        campaignSettingsRepository.save(settings);
    }

    /**
     * Stops the run in progress today, freezing its score at today rather than at its settlement day.
     *
     * <p>Closes the run exactly as the rollover would, and additionally marks it stopped so
     * {@link Run#finalDay()} reads today instead of a settlement day it will now never reach. The
     * colony still has to be replayed after this returns — {@code ColonyReplayService} is not called
     * from here, the same separation {@link #ensureRunFor} already keeps from the colony it bounds.
     *
     * @return the stopped run
     * @throws ConflictException when no run is currently open
     */
    @Transactional
    public Run stopCurrentRun() {
        Run run = currentRun().orElseThrow(
            () -> new ConflictException("No campaign is currently running.")
        );

        LocalDate today = LocalDate.now(clock.withZone(weekCalendar.zone()));
        run.setStoppedOn(today);
        run.setClosedAt(clock.instant());
        Run stopped = runRepository.save(run);

        LOGGER.info(
            "Run {} stopped on {} by an operator, short of its settlement day {}.",
            stopped.getNumber(),
            today,
            stopped.settlementDay()
        );

        return stopped;
    }

    /**
     * Starts a new run on this week's Monday, for an operator to use once automatic renewal is off
     * and no run is open.
     *
     * <p>Not the path a live campaign takes: that one is opened lazily, from whichever page loads
     * first once the calendar has moved past the previous run's own end. This one exists for the gap
     * automatic renewal being off deliberately leaves open, which nothing would otherwise ever fill.
     *
     * <p>This week's Monday even when the run just stopped opened on it too. The alternative — the
     * Monday after — would hand back a run whose first day is in the future, which every colony
     * reader would then have to make sense of, for a campaign nobody can play until the week turns.
     *
     * @return the started run
     * @throws ConflictException when a run is already open
     */
    @Transactional
    public Run startRunNow() {
        if (currentRun().isPresent()) {
            throw new ConflictException("A campaign is already running.");
        }

        return ensureRunFor(weekCalendar.currentWeekStart());
    }

    /**
     * Returns one run by its identifier.
     *
     * @param id run identifier
     * @return the run
     * @throws ResourceNotFoundException when no run holds that identifier
     */
    @Transactional(readOnly = true)
    public Run findRun(long id) {
        return runRepository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("Campaign " + id + " does not exist.")
        );
    }

    /**
     * Deletes one run, once whatever hangs off it has been deleted too.
     *
     * <p>Deliberately takes the run rather than its identifier: the caller has to have loaded it,
     * which is also when it clears the run's snapshots and its boss encounters — this method would
     * otherwise fail on their foreign keys. {@code CampaignAdminService} is that caller.
     *
     * @param run run to delete, must not be {@code null}
     */
    @Transactional
    public void deleteRun(Run run) {
        Objects.requireNonNull(run, "run must not be null");

        runRepository.delete(run);

        LOGGER.warn(
            "Run {} was deleted by an operator, along with its colony and its fights.",
            run.getNumber()
        );
    }

    /**
     * Reads the campaign's single settings row, creating it if a deployment predates the table's
     * seed row.
     *
     * @return the campaign settings
     */
    private CampaignSettings settings() {
        return campaignSettingsRepository.findById(CampaignSettings.SINGLETON_ID)
            .orElseGet(() -> campaignSettingsRepository.save(new CampaignSettings()));
    }

    /**
     * Returns the run covering a week, opening and closing runs as needed to reach it.
     *
     * <p>Idempotent: a week the open run already covers returns it untouched.
     *
     * <p>The loop is what makes this survive an outage. {@code DefaultWeeklyRolloverService} catches up
     * every week it missed in one pass and then opens the current one once, so a rollover firing after
     * three weeks of downtime can be several runs behind. Each iteration closes the run that has expired
     * and opens the next one on the Monday immediately after it, which is also what keeps runs
     * contiguous — the property the settlement day depends on, since a run's seventy-first day is the
     * next run's first.
     *
     * @param weekStart Monday identifying the week to cover, must not be {@code null}
     * @return the run covering that week
     */
    @Transactional
    public Run ensureRunFor(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "weekStart must not be null");

        if (!weekCalendar.isWeekStart(weekStart)) {
            throw new IllegalArgumentException("A run must be resolved from a Monday.");
        }

        Optional<Run> openRun = runRepository.findByClosedAtIsNull();
        if (openRun.isEmpty()) {
            return openRun(nextRunNumber(), weekStart);
        }

        Run run = openRun.orElseThrow();
        while (weekStart.isAfter(run.getLastWeekStart())) {
            run.setClosedAt(clock.instant());
            runRepository.save(run);

            LOGGER.info(
                "Run {} closed after its week {}, final score is its settlement day {}.",
                run.getNumber(),
                run.getLastWeekStart(),
                run.settlementDay()
            );

            run = openRun(run.getNumber() + 1, run.getLastWeekStart().plusWeeks(1));
        }

        return run;
    }

    /**
     * Returns the number the next run takes.
     *
     * <p>Read off the highest run ever opened rather than assumed to be one when nothing is open.
     * Runs are normally contiguous — closing one opens its successor in the same transaction — but a
     * database whose last run was closed on its own would otherwise have run one opened a second
     * time, which the unique numbering rejects.
     *
     * @return the next sequential run number
     */
    private int nextRunNumber() {
        return runRepository.findTopByOrderByNumberDesc()
            .map(run -> run.getNumber() + 1)
            .orElse(FIRST_RUN_NUMBER);
    }

    /**
     * Opens one run on a week, freezing the roster size it will be measured against.
     *
     * <p>Inserted with {@code ON CONFLICT DO NOTHING} and read back, rather than saved through the
     * entity manager. Several requests open a run lazily — the colony's three endpoints and the boss
     * endpoint all do — and the page fires them in parallel, so on the very first load two of them
     * read "no run yet" from the same snapshot and both go on to insert. Letting Postgres arbitrate
     * turns that race into a no-op for the loser instead of a constraint violation that fails a
     * perfectly ordinary page load.
     *
     * <p>Read back by <b>the run left open</b>, never by the week inserted. A week can carry two runs
     * — one an operator stopped on it, and the clean one opened in its place — and reading the week
     * back handed out whichever of them the unique index happened to hold, which after a stop was the
     * closed one. A caller then went on with a run that {@link #currentRun()} does not even report.
     *
     * @param number    sequential run number
     * @param weekStart Monday the run's first week starts on
     * @return the run covering that week, whether this call created it or lost the race
     */
    private Run openRun(int number, LocalDate weekStart) {
        int rosterSize = Math.max(
            MINIMUM_ROSTER_SIZE,
            (int) playerRepository.countByStatus(Player.COMPETITIVE_STATUS)
        );
        LocalDate lastWeekStart = weekStart.plusWeeks(ruleset.runLengthWeeks() - 1L);

        runRepository.insertIfAbsent(number, weekStart, lastWeekStart, rosterSize);

        // The insert is native, so it bypasses the persistence context: the entity has to be read
        // back for the caller to get a managed instance, and that read is also what returns the
        // winner's row when this call lost the race.
        Run run = runRepository.findByClosedAtIsNull()
            .orElseThrow(() -> new IllegalStateException(
                "Run " + number + " could not be opened on week " + weekStart
                    + ": a run already holds that number."
            ));

        LOGGER.info(
            "Run {} is open on week {}, running through week {} with a roster of {}.",
            run.getNumber(),
            run.getFirstWeekStart(),
            run.getLastWeekStart(),
            run.getRosterSize()
        );

        return run;
    }
}
