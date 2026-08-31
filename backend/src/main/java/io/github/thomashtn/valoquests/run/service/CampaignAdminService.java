package io.github.thomashtn.valoquests.run.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse.CampaignRunSummary;
import io.github.thomashtn.valoquests.run.dto.CampaignRunStatus;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service backing the campaign lifecycle's admin endpoints: listing every run, starting
 * and stopping the current one, and switching automatic renewal.
 *
 * <p>Sits above both {@link RunService} and {@link ColonyReplayService} rather than folding into
 * either: {@code RunService} cannot depend on the colony it bounds, and only this layer needs both —
 * stopping a run has to freeze its score immediately, not wait for the next scheduled tick.
 */
@Service
public class CampaignAdminService {

    /**
     * Service owning the run itself: which one is open, and when it gives way to the next.
     */
    private final RunService runService;

    /**
     * Service replaying a run's colony once its lifecycle changes.
     */
    private final ColonyReplayService colonyReplayService;

    /**
     * Repository reading each run's own score off its last snapshot.
     */
    private final ColonyDailySnapshotRepository snapshotRepository;

    /**
     * Repository holding the fights a run has to give back when it stops, or take with it when it is
     * deleted.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Calendar placing the day a run stopped in its week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the campaign admin service.
     *
     * @param runService          run service
     * @param colonyReplayService colony replay service
     * @param snapshotRepository  colony daily snapshot repository
     * @param encounterRepository weekly boss encounter repository
     * @param weekCalendar        week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignAdminService(
        RunService runService,
        ColonyReplayService colonyReplayService,
        ColonyDailySnapshotRepository snapshotRepository,
        WeeklyBossEncounterRepository encounterRepository,
        WeekCalendar weekCalendar
    ) {
        this.runService = runService;
        this.colonyReplayService = colonyReplayService;
        this.snapshotRepository = snapshotRepository;
        this.encounterRepository = encounterRepository;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Lists every run of the campaign, current first, and the automatic-renewal setting.
     *
     * @return the campaign's lifecycle
     */
    @Transactional(readOnly = true)
    public CampaignAdminResponse findCampaigns() {
        List<CampaignRunSummary> runs = new ArrayList<>();
        runService.currentRun().ifPresent(run -> runs.add(toSummary(run, CampaignRunStatus.RUNNING)));
        runService.closedRuns().forEach(run -> runs.add(toSummary(
            run,
            run.getStoppedOn() != null ? CampaignRunStatus.STOPPED : CampaignRunStatus.COMPLETED
        )));

        return new CampaignAdminResponse(runService.isAutoRenewEnabled(), runs);
    }

    /**
     * Switches automatic renewal on or off.
     *
     * @param enabled whether the weekly rollover may open a new run on its own
     */
    @Transactional
    public void setAutoRenewEnabled(boolean enabled) {
        runService.setAutoRenewEnabled(enabled);
    }

    /**
     * Starts a new run today, for the gap automatic renewal being off deliberately leaves open.
     *
     * @return the started run, summarized
     */
    @Transactional
    public CampaignRunSummary startCampaign() {
        return toSummary(runService.startRunNow(), CampaignRunStatus.RUNNING);
    }

    /**
     * Stops the run in progress today, and replays its colony immediately so the frozen score is
     * ready as soon as this returns rather than at the next scheduled tick.
     *
     * <p>Also gives back the fights the run stops short of settling. A run is credited a week's boss
     * on the rollover that follows it, which a run stopped mid-week never reaches, so the encounter
     * it leaves behind pays nobody — and an encounter is unique per week, so leaving it there would
     * deny the campaign opened in its place a boss of its own for that same week.
     *
     * @return the stopped run, summarized
     */
    @Transactional
    public CampaignRunSummary stopCampaign() {
        Run stopped = runService.stopCurrentRun();

        encounterRepository.deleteAll(
            encounterRepository.findAllByRunIdAndFinalizedAtIsNullAndWeekStartGreaterThanEqual(
                stopped.getId(),
                weekCalendar.weekStartOf(stopped.finalDay())
            )
        );

        colonyReplayService.replay(stopped);

        return toSummary(stopped, CampaignRunStatus.STOPPED);
    }

    /**
     * Deletes one campaign: the run, the colony it grew and the fights it drew.
     *
     * <p>Everything else a week produced stays. The weekly challenges and their progress, the weekly
     * rankings and the imported matches all belong to the other pillar — they are read by the weekly
     * ranking whether a campaign was running or not — and deleting the campaign that happened to be
     * running over those weeks is not a statement about them. {@code CampaignResetService} is the
     * operation that clears those, and it clears all of them.
     *
     * <p>The run in progress can be deleted like any other. It is what an operator reaches for after
     * opening a campaign by mistake, and the alternative — stopping it first — would leave exactly
     * the closed one-day run they are trying to get rid of.
     *
     * @param id identifier of the run to delete
     */
    @Transactional
    public void deleteCampaign(long id) {
        Run run = runService.findRun(id);

        encounterRepository.deleteAllByRunId(id);
        snapshotRepository.deleteAllByRunId(id);

        // Both deletions above are queued in the persistence context; flushing them here is what
        // guarantees they reach the database before the run they point at, rather than in whatever
        // order the commit would have picked on its own.
        snapshotRepository.flush();

        runService.deleteRun(run);
    }

    /**
     * Summarizes one run for the admin endpoint, reading its score off its own last snapshot.
     *
     * @param run    run to summarize
     * @param status the run's own place in the lifecycle
     * @return the run's summary
     */
    private CampaignRunSummary toSummary(Run run, CampaignRunStatus status) {
        return new CampaignRunSummary(
            run.getId(),
            run.getFirstWeekStart(),
            run.finalDay(),
            run.getRosterSize(),
            status,
            latestPopulation(run)
        );
    }

    /**
     * Reads a run's own most recent population, off its last snapshot.
     *
     * @param run run to read
     * @return the last snapshot's population, or zero before the run has a single snapshot yet
     */
    private int latestPopulation(Run run) {
        List<ColonyDailySnapshot> snapshots = snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());

        return Optional.ofNullable(snapshots.isEmpty() ? null : snapshots.getLast())
            .map(snapshot -> (int) Math.round(snapshot.getPopulation().doubleValue()))
            .orElse(0);
    }
}
