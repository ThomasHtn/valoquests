package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayInputs;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayResult;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies which campaign is replayed, and how far.
 */
@ExtendWith(MockitoExtension.class)
class CampaignReplayServiceTest {

    /**
     * Empty inputs the engine is handed in these cases.
     */
    private static final CampaignReplayInputs NO_INPUTS =
        new CampaignReplayInputs(List.of(), List.of(), Map.of(), Map.of(), List.of());

    /**
     * Empty result the engine hands back.
     */
    private static final CampaignReplayResult NO_RESULT = new CampaignReplayResult(List.of(), List.of());

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignWeekRepository weekRepository;

    @Mock
    private CampaignReplayInputAssembler assembler;

    @Mock
    private CampaignReplayEngine engine;

    @Mock
    private CampaignReplayWriter writer;

    @Test
    @DisplayName("Replays the running campaign up to today")
    void shouldReplayUpToToday() {
        LocalDate today = CampaignFixtures.FIRST_WEEK_START.plusDays(3);
        CampaignReplayService service = serviceOn(today);
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        List<CampaignWeek> weeks = List.of(CampaignFixtures.week(campaign, 1, 1_000, 50));

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(weeks);
        when(assembler.assemble(campaign, weeks, today, today.minusDays(1))).thenReturn(NO_INPUTS);
        when(engine.replay(anyList(), anyList())).thenReturn(NO_RESULT);

        assertThat(service.replayRunningCampaign()).contains(NO_RESULT);
        verify(writer).write(campaign, weeks, NO_INPUTS, NO_RESULT);
    }

    @Test
    @DisplayName("Never replays a campaign past its own final day")
    void shouldStopAtTheFinalDay() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        CampaignReplayService service = serviceOn(campaign.finalDay().plusMonths(2));

        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of());
        when(assembler.assemble(eq(campaign), anyList(), eq(campaign.finalDay()), eq(campaign.finalDay())))
            .thenReturn(NO_INPUTS);
        when(engine.replay(anyList(), anyList())).thenReturn(NO_RESULT);

        service.replay(campaign);

        verify(assembler).assemble(campaign, List.of(), campaign.finalDay(), campaign.finalDay());
    }

    @Test
    @DisplayName("Settles a Sunday only from the day after it")
    void shouldSettleASundayTheDayAfter() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        LocalDate sunday = CampaignFixtures.FIRST_WEEK_START.plusDays(6);

        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of());
        when(assembler.assemble(any(), anyList(), any(), any())).thenReturn(NO_INPUTS);
        when(engine.replay(anyList(), anyList())).thenReturn(NO_RESULT);

        serviceOn(sunday).replay(campaign);
        serviceOn(sunday.plusDays(1)).replay(campaign);

        verify(assembler).assemble(campaign, List.of(), sunday, sunday.minusDays(1));
        verify(assembler).assemble(campaign, List.of(), sunday.plusDays(1), sunday);
    }

    @Test
    @DisplayName("Freezes a campaign an operator stopped at the day it stopped on")
    void shouldStopAtTheDayItWasStoppedOn() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStoppedOn(CampaignFixtures.FIRST_WEEK_START.plusDays(10));
        CampaignReplayService service = serviceOn(campaign.getLastWeekStart());

        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of());
        when(assembler.assemble(any(), anyList(), any(), any())).thenReturn(NO_INPUTS);
        when(engine.replay(anyList(), anyList())).thenReturn(NO_RESULT);

        service.replay(campaign);

        verify(assembler).assemble(campaign, List.of(), campaign.getStoppedOn(), campaign.getStoppedOn());
    }

    @Test
    @DisplayName("Replays nothing while the campaign has only been opened")
    void shouldNotReplayAnOpenedCampaign() {
        CampaignReplayService service = serviceOn(CampaignFixtures.FIRST_WEEK_START.minusDays(2));
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        assertThat(service.replayRunningCampaign()).isEmpty();
        verifyNoInteractions(assembler, engine, writer);
    }

    @Test
    @DisplayName("Replays nothing between two campaigns")
    void shouldNotReplayWithoutACampaign() {
        CampaignReplayService service = serviceOn(CampaignFixtures.FIRST_WEEK_START);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());

        assertThat(service.replayRunningCampaign()).isEmpty();
        verify(weekRepository, never()).findAllByCampaignIdOrderByWeekIndexAsc(any());
    }

    /**
     * Builds the service on a clock frozen on one day.
     *
     * @param today day the service reads as today
     * @return the service
     */
    private CampaignReplayService serviceOn(LocalDate today) {
        Instant instant = today.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3_600);
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);

        return new CampaignReplayService(
            campaignRepository,
            weekRepository,
            assembler,
            engine,
            writer,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }
}
