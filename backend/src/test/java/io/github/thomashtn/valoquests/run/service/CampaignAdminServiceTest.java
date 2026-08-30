package io.github.thomashtn.valoquests.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse.CampaignRunSummary;
import io.github.thomashtn.valoquests.run.dto.CampaignRunStatus;
import io.github.thomashtn.valoquests.run.entity.Run;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CampaignAdminService}, the layer sitting above {@link RunService} and
 * {@link ColonyReplayService} for the campaign lifecycle's admin endpoints.
 */
class CampaignAdminServiceTest {

    private RunService runService;

    private ColonyReplayService colonyReplayService;

    private ColonyDailySnapshotRepository snapshotRepository;

    private CampaignAdminService service;

    @BeforeEach
    void setUp() {
        runService = mock(RunService.class);
        colonyReplayService = mock(ColonyReplayService.class);
        snapshotRepository = mock(ColonyDailySnapshotRepository.class);

        service = new CampaignAdminService(runService, colonyReplayService, snapshotRepository);
    }

    @Test
    @DisplayName("lists the running campaign first, then the closed ones, with the auto-renew setting")
    void shouldListTheRunningCampaignFirstThenClosedOnes() {
        Run running = run(1L, 100, LocalDate.of(2026, 6, 1), null);
        Run completed = run(2L, 101, LocalDate.of(2026, 3, 1), null);
        Run stopped = run(3L, 102, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 10));

        when(runService.currentRun()).thenReturn(Optional.of(running));
        when(runService.closedRuns()).thenReturn(List.of(completed, stopped));
        when(runService.isAutoRenewEnabled()).thenReturn(false);
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(100L)).thenReturn(snapshots(24));
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(101L)).thenReturn(snapshots(48));
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(102L)).thenReturn(snapshots(30));

        CampaignAdminResponse response = service.findCampaigns();

        assertThat(response.autoRenewEnabled()).isFalse();
        assertThat(response.runs())
            .extracting(CampaignRunSummary::id, CampaignRunSummary::status, CampaignRunSummary::score)
            .containsExactly(
                tuple(1L, CampaignRunStatus.RUNNING, 24),
                tuple(2L, CampaignRunStatus.COMPLETED, 48),
                tuple(3L, CampaignRunStatus.STOPPED, 30)
            );
    }

    @Test
    @DisplayName("stops the running campaign and replays its colony before returning")
    void shouldStopTheRunningCampaignAndReplayItsColony() {
        Run stopped = run(1L, 100, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15));
        when(runService.stopCurrentRun()).thenReturn(stopped);
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(100L)).thenReturn(snapshots(19));

        CampaignRunSummary summary = service.stopCampaign();

        verify(colonyReplayService).replay(stopped);
        assertThat(summary.status()).isEqualTo(CampaignRunStatus.STOPPED);
        assertThat(summary.score()).isEqualTo(19);
    }

    @Test
    @DisplayName("starts a campaign through the run service and reports it as running")
    void shouldStartACampaignAndReportItAsRunning() {
        Run started = run(1L, 100, LocalDate.of(2026, 6, 1), null);
        when(runService.startRunNow()).thenReturn(started);
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(100L)).thenReturn(List.of());

        CampaignRunSummary summary = service.startCampaign();

        assertThat(summary.status()).isEqualTo(CampaignRunStatus.RUNNING);
        assertThat(summary.score()).isZero();
    }

    private static Run run(Long id, int number, LocalDate firstWeekStart, LocalDate stoppedOn) {
        Run run = new Run();
        run.setId(id);
        run.setNumber(number);
        run.setFirstWeekStart(firstWeekStart);
        run.setLastWeekStart(firstWeekStart.plusWeeks(9));
        run.setRosterSize(6);
        run.setStoppedOn(stoppedOn);
        return run;
    }

    private static List<ColonyDailySnapshot> snapshots(int lastPopulation) {
        ColonyDailySnapshot snapshot = new ColonyDailySnapshot();
        snapshot.setPopulation(BigDecimal.valueOf(lastPopulation));
        return List.of(snapshot);
    }
}
