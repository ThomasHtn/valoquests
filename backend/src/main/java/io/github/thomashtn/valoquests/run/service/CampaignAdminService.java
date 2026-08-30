package io.github.thomashtn.valoquests.run.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse.CampaignRunSummary;
import io.github.thomashtn.valoquests.run.dto.CampaignRunStatus;
import io.github.thomashtn.valoquests.run.entity.Run;
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
     * Creates the campaign admin service.
     *
     * @param runService         run service
     * @param colonyReplayService colony replay service
     * @param snapshotRepository  colony daily snapshot repository
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignAdminService(
        RunService runService,
        ColonyReplayService colonyReplayService,
        ColonyDailySnapshotRepository snapshotRepository
    ) {
        this.runService = runService;
        this.colonyReplayService = colonyReplayService;
        this.snapshotRepository = snapshotRepository;
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
     * @return the stopped run, summarized
     */
    @Transactional
    public CampaignRunSummary stopCampaign() {
        Run stopped = runService.stopCurrentRun();
        colonyReplayService.replay(stopped);

        return toSummary(stopped, CampaignRunStatus.STOPPED);
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
