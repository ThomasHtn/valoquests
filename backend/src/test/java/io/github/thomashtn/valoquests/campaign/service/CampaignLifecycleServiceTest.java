package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.NewCampaign;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
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
class CampaignLifecycleServiceTest {

    /**
     * Friday the campaign is opened on.
     */
    private static final Instant OPENING_DAY = Instant.parse("2026-09-04T10:00:00Z");

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
    private PlayerRepository playerRepository;

    @Mock
    private SquadCalibrationService calibrationService;

    @Mock
    private CampaignFactory factory;

    @Test
    @DisplayName("Opens a campaign starting the Monday after today")
    void shouldOpenACampaignStartingNextMonday() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Player operator = CampaignFixtures.player(1, "Alpha");
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of(operator));
        when(calibrationService.calibrate(List.of(operator), LocalDate.of(2026, 9, 4)))
            .thenReturn(calibration());
        when(factory.build(anyInt(), anyList(), any(), any()))
            .thenReturn(new NewCampaign(campaign, List.of(), List.of()));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        Campaign opened = service.open();

        assertThat(opened.getStatus()).isEqualTo(CampaignStatus.OPENED);
        verify(factory).build(1, List.of(operator), calibration(), FIRST_WEEK_START);
        verify(campaignPlayerRepository).saveAll(List.of());
        verify(campaignWeekRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("Numbers a new campaign one past the last one ever opened")
    void shouldNumberTheNextCampaign() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Player operator = CampaignFixtures.player(1, "Alpha");
        Campaign previous = CampaignFixtures.runningCampaign(1);
        previous.setNumber(4);
        Campaign campaign = CampaignFixtures.runningCampaign(2);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of(operator));
        when(calibrationService.calibrate(anyList(), any())).thenReturn(calibration());
        when(campaignRepository.findFirstByOrderByNumberDesc()).thenReturn(Optional.of(previous));
        when(factory.build(anyInt(), anyList(), any(), any()))
            .thenReturn(new NewCampaign(campaign, List.of(), List.of()));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        service.open();

        verify(factory).build(5, List.of(operator), calibration(), FIRST_WEEK_START);
    }

    @Test
    @DisplayName("Refuses to open a second campaign while one is live")
    void shouldRefuseASecondLiveCampaign() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED))
            .thenReturn(Optional.of(CampaignFixtures.runningCampaign(1)));

        assertThatThrownBy(service::open)
            .isInstanceOf(CampaignLifecycleException.class)
            .hasMessageContaining("already opened or running");
    }

    @Test
    @DisplayName("Refuses to open a campaign nobody is active for")
    void shouldRefuseAnEmptyRoster() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(service::open)
            .isInstanceOf(CampaignLifecycleException.class)
            .hasMessageContaining("No operator is active");
    }

    @Test
    @DisplayName("Starts an opened campaign once its first Monday has come")
    void shouldStartOnTheFirstMonday() {
        CampaignLifecycleService service = serviceAt(Instant.parse("2026-09-07T00:10:00Z"));
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        assertThat(service.startIfDue()).contains(campaign);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    @DisplayName("Leaves an opened campaign waiting until its Monday")
    void shouldNotStartBeforeTheFirstMonday() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

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

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        assertThat(service.closeIfComplete(Clock.fixed(monday, ZoneOffset.UTC))).contains(campaign);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(campaign.getClosedAt()).isEqualTo(monday);
    }

    @Test
    @DisplayName("Leaves a campaign open on its own final day")
    void shouldNotCloseOnTheFinalDay() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        Instant sunday = campaign.finalDay().atStartOfDay(ZoneOffset.UTC).toInstant();
        CampaignLifecycleService service = serviceAt(sunday);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        assertThat(service.closeIfComplete(Clock.fixed(sunday, ZoneOffset.UTC))).isEmpty();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    @DisplayName("Freezes a stopped campaign on the last day that is actually over")
    void shouldStopOnYesterday() {
        Instant now = Instant.parse("2026-09-16T18:00:00Z");
        CampaignLifecycleService service = serviceAt(now);
        Campaign campaign = CampaignFixtures.runningCampaign(1);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        Campaign stopped = service.stop(Clock.fixed(now, ZoneOffset.UTC));

        assertThat(stopped.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(stopped.getStoppedOn()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(stopped.finalDay()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    @DisplayName("Refuses to stop when no campaign is live")
    void shouldRefuseToStopNothing() {
        CampaignLifecycleService service = serviceAt(OPENING_DAY);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.stop(Clock.fixed(OPENING_DAY, ZoneOffset.UTC)))
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

        return new CampaignLifecycleService(
            campaignRepository,
            campaignPlayerRepository,
            campaignWeekRepository,
            playerRepository,
            calibrationService,
            factory,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    /**
     * Builds the calibration the fixtures are sized on.
     *
     * @return the calibration
     */
    private SquadCalibration calibration() {
        return new SquadCalibration(
            CampaignFixtures.REFERENCE,
            CampaignTier.NORMAL,
            ChallengeScaling.NONE,
            9,
            FIRST_WEEK_START.minusMonths(9),
            List.of()
        );
    }
}
