package io.github.thomashtn.valoquests.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse.CampaignRunSummary;
import io.github.thomashtn.valoquests.run.dto.CampaignRunStatus;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Tests {@link CampaignAdminService}, the layer sitting above {@link RunService} and
 * {@link ColonyReplayService} for the campaign lifecycle's admin endpoints.
 */
class CampaignAdminServiceTest {

    private RunService runService;

    private ColonyReplayService colonyReplayService;

    private ColonyDailySnapshotRepository snapshotRepository;

    private WeeklyBossEncounterRepository encounterRepository;

    private CampaignAdminService service;

    @BeforeEach
    void setUp() {
        runService = mock(RunService.class);
        colonyReplayService = mock(ColonyReplayService.class);
        snapshotRepository = mock(ColonyDailySnapshotRepository.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);

        service = new CampaignAdminService(
            runService,
            colonyReplayService,
            snapshotRepository,
            encounterRepository,
            new WeekCalendar(
                Clock.fixed(Instant.parse("2026-06-15T09:00:00Z"), ZoneOffset.UTC),
                ZoneOffset.UTC
            )
        );
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
        // Keyed by the run's id, which is what the snapshots carry — not by its display number.
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(1L)).thenReturn(snapshots(24));
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(2L)).thenReturn(snapshots(48));
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(3L)).thenReturn(snapshots(30));

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
        when(snapshotRepository.findAllByRunIdOrderByDayAsc(1L)).thenReturn(snapshots(19));

        CampaignRunSummary summary = service.stopCampaign();

        verify(colonyReplayService).replay(stopped);
        assertThat(summary.status()).isEqualTo(CampaignRunStatus.STOPPED);
        assertThat(summary.score()).isEqualTo(19);
    }

    @Test
    @DisplayName("gives back the fights the stopped campaign never settled, from its stop week on")
    void shouldReleaseTheFightsTheStoppedCampaignNeverSettled() {
        // Stopped on Monday 15 June, so the week released is that Monday's — its own fight is still
        // open, pays nobody, and would deny the next campaign a boss for the same week.
        Run stopped = run(1L, 100, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15));
        List<WeeklyBossEncounter> unsettled = List.of(new WeeklyBossEncounter());
        when(runService.stopCurrentRun()).thenReturn(stopped);
        when(encounterRepository.findAllByRunIdAndFinalizedAtIsNullAndWeekStartGreaterThanEqual(
            1L,
            LocalDate.of(2026, 6, 15)
        )).thenReturn(unsettled);

        service.stopCampaign();

        verify(encounterRepository).deleteAll(unsettled);
    }

    @Test
    @DisplayName("deletes a campaign with the colony it grew and the fights it drew")
    void shouldDeleteACampaignWithItsColonyAndItsFights() {
        Run run = run(7L, 106, LocalDate.of(2026, 6, 1), null);
        when(runService.findRun(7L)).thenReturn(run);

        service.deleteCampaign(7L);

        InOrder order = inOrder(encounterRepository, snapshotRepository, runService);
        order.verify(encounterRepository).deleteAllByRunId(7L);
        order.verify(snapshotRepository).deleteAllByRunId(7L);
        // Flushed before the run itself, or the commit could delete the row its dependents point at.
        order.verify(snapshotRepository).flush();
        order.verify(runService).deleteRun(run);
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
