package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignStartWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.NewCampaign;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that a campaign only ever starts, closes and stops when a person or the calendar says so.
 */
@ExtendWith(MockitoExtension.class)
class DefaultCampaignLifecycleServiceTest {

    /**
     * Friday the campaign is opened on.
     */
    private static final Instant OPENING_DAY = Instant.parse("2026-09-04T10:00:00Z");

    /**
     * Day the campaign is opened on.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 4);

    /**
     * Monday the campaign starts on.
     */
    private static final LocalDate FIRST_WEEK_START = CampaignFixtures.FIRST_WEEK_START;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignPlayerRepository campaignPlayerRepository;

    @Mock
    private CampaignWeekRepository campaignWeekRepository;

    @Mock
    private CampaignFactory factory;

    @Mock
    private CampaignReplayService replayService;

    @Test
    @DisplayName("Opens a campaign starting the Monday after today")
    void shouldOpenACampaignStartingNextMonday() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findLive()).thenReturn(Optional.empty());
        when(factory.build(anyInt(), any(), any(), any()))
            .thenReturn(new NewCampaign(campaign, List.of(), List.of()));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        Campaign opened = service.open(CampaignDifficulty.AMATEUR, CampaignStartWeek.NEXT_WEEK);

        assertThat(opened.getStatus()).isEqualTo(CampaignStatus.OPENED);
        verify(factory).build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, TODAY);
        verify(campaignPlayerRepository).saveAll(List.of());
        verify(campaignWeekRepository).saveAll(List.of());
        verify(replayService, never()).replay(any());
    }

    @Test
    @DisplayName("Opens a campaign on the Monday of the week in progress and replays the days already played")
    void shouldOpenACampaignOnTheCurrentWeek() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign campaign = CampaignFixtures.runningCampaign(1);

        when(campaignRepository.findLive()).thenReturn(Optional.empty());
        when(factory.build(anyInt(), any(), any(), any()))
            .thenReturn(new NewCampaign(campaign, List.of(), List.of()));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        Campaign opened = service.open(CampaignDifficulty.AMATEUR, CampaignStartWeek.CURRENT_WEEK);

        assertThat(opened.getStatus()).isEqualTo(CampaignStatus.RUNNING);
        verify(factory).build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START.minusWeeks(1), TODAY);
        verify(replayService).replay(campaign);
    }

    @Test
    @DisplayName("Numbers a new campaign one past the last one ever opened")
    void shouldNumberTheNextCampaign() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign previous = CampaignFixtures.runningCampaign(1);
        previous.setNumber(4);
        Campaign campaign = CampaignFixtures.runningCampaign(2);

        when(campaignRepository.findLive()).thenReturn(Optional.empty());
        when(campaignRepository.findFirstByOrderByNumberDesc()).thenReturn(Optional.of(previous));
        when(factory.build(anyInt(), any(), any(), any()))
            .thenReturn(new NewCampaign(campaign, List.of(), List.of()));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        service.open(CampaignDifficulty.AMATEUR, CampaignStartWeek.NEXT_WEEK);

        verify(factory).build(5, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, TODAY);
    }

    @Test
    @DisplayName("Refuses to open a second campaign while one is live")
    void shouldRefuseASecondLiveCampaign() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);

        when(campaignRepository.findLive())
            .thenReturn(Optional.of(CampaignFixtures.runningCampaign(1)));

        assertThatThrownBy(() -> service.open(CampaignDifficulty.AMATEUR, CampaignStartWeek.NEXT_WEEK))
            .isInstanceOf(CampaignLifecycleException.class)
            .hasMessageContaining("already opened or running");
    }

    @Test
    @DisplayName("Starts an opened campaign once its first Monday has come")
    void shouldStartOnTheFirstMonday() {
        CampaignLifecycleService service = serviceAt(Instant.parse("2026-09-07T00:10:00Z"));
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));

        assertThat(service.startIfDue()).contains(campaign);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    @DisplayName("Leaves an opened campaign waiting until its Monday")
    void shouldNotStartBeforeTheFirstMonday() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));

        assertThat(service.startIfDue()).isEmpty();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.OPENED);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("Closes a campaign only once its tenth Sunday is behind")
    void shouldCloseAfterTheFinalDay() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        Instant monday = campaign.finalDay().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        CampaignLifecycleService service = serviceAt(monday);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));

        assertThat(service.closeIfComplete()).contains(campaign);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(campaign.getClosedAt()).isEqualTo(monday);
    }

    @Test
    @DisplayName("Leaves a campaign open on its own final day")
    void shouldNotCloseOnTheFinalDay() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        Instant sunday = campaign.finalDay().atStartOfDay(ZoneOffset.UTC).toInstant();
        CampaignLifecycleService service = serviceAt(sunday);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));

        assertThat(service.closeIfComplete()).isEmpty();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    @DisplayName("Freezes a stopped campaign on the last day that is actually over")
    void shouldStopOnYesterday() {
        Instant now = Instant.parse("2026-09-16T18:00:00Z");
        CampaignLifecycleService service = serviceAt(now);
        Campaign campaign = CampaignFixtures.runningCampaign(1);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));

        Campaign stopped = service.stop();

        assertThat(stopped.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(stopped.getStoppedOn()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(stopped.finalDay()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    @DisplayName("Rebuilds a running campaign up to its stop day before freezing it, dropping today's half day")
    void shouldReplayUpToTheStopDayBeforeFreezing() {
        Instant now = Instant.parse("2026-09-16T18:00:00Z");
        CampaignLifecycleService service = serviceAt(now);
        Campaign campaign = CampaignFixtures.runningCampaign(1);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));
        when(replayService.replay(campaign)).thenAnswer(invocation -> {
            assertThat(campaign.finalDay()).isEqualTo(LocalDate.of(2026, 9, 15));
            return null;
        });

        service.stop();

        verify(replayService).replay(campaign);
    }

    @Test
    @DisplayName("Closes an opened campaign that never started without replaying it")
    void shouldNotReplayACampaignThatNeverStarted() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findLive()).thenReturn(Optional.of(campaign));

        service.stop();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        verify(replayService, never()).replay(any());
    }

    @Test
    @DisplayName("Refuses to stop when no campaign is live")
    void shouldRefuseToStopNothing() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);

        when(campaignRepository.findLive()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stop())
            .isInstanceOf(CampaignLifecycleException.class)
            .hasMessageContaining("nothing to stop");
    }

    @Test
    @DisplayName("Refuses to delete a campaign that does not exist")
    void shouldRefuseToDeleteAnUnknownCampaign() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);

        when(campaignRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(42L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("42");
    }

    @Test
    @DisplayName("Deletes a campaign along with everything it owns")
    void shouldDeleteACampaign() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign campaign = CampaignFixtures.runningCampaign(1);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        service.delete(1L);

        verify(campaignRepository).delete(campaign);
    }

    /**
     * Builds the service on a clock frozen at one instant.
     *
     * @param now instant the service reads as now
     * @return the service
     */
    private CampaignLifecycleService serviceAt(Instant now) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        return new DefaultCampaignLifecycleService(
            campaignRepository,
            campaignPlayerRepository,
            campaignWeekRepository,
            factory,
            new WeekCalendar(clock, ZoneOffset.UTC),
            replayService,
            clock
        );
    }
}
