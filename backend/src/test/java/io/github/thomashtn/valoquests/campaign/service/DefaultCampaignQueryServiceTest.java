package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekResponse;
import io.github.thomashtn.valoquests.campaign.dto.FatalBlowResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that the public reading only ever repeats what the last replay stored.
 */
@ExtendWith(MockitoExtension.class)
class DefaultCampaignQueryServiceTest {

    /**
     * Wednesday of the campaign's second week.
     */
    private static final LocalDate TODAY = CampaignFixtures.FIRST_WEEK_START.plusDays(9);

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignWeekRepository weekRepository;

    @Mock
    private CampaignDailySnapshotRepository snapshotRepository;

    @Mock
    private CampaignDayReader dayReader;

    private DefaultCampaignQueryService service;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3_600), ZoneOffset.UTC);
        service = new DefaultCampaignQueryService(
            campaignRepository,
            weekRepository,
            snapshotRepository,
            dayReader,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
        campaign = CampaignFixtures.runningCampaign(1);
    }

    @Test
    @DisplayName("Reads the base from the last day the replay computed")
    void shouldReadTheBaseFromTheLastDay() {
        live();
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of());
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(1L))
            .thenReturn(List.of(snapshot(TODAY.minusDays(1), 500, 100, 40), snapshot(TODAY, 1_000, 2_000, 1_400)));

        CampaignResponse response = service.currentCampaign();

        assertThat(response.status()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(response.currentWeekIndex()).isEqualTo(2);
        assertThat(response.base().population()).isEqualTo(1_000);
        assertThat(response.base().foodStock()).isEqualTo(2_000);
        assertThat(response.base().componentsStock()).isEqualTo(1_400);
        assertThat(response.base().dailyUpkeep()).isEqualTo(8);
        assertThat(response.base().protectedFood()).isEqualTo(56);
        assertThat(response.base().rescuesByComponents()).isEqualTo(100);
        assertThat(response.base().rescuesByFood()).isEqualTo(162);
        assertThat(response.base().populationChange()).isEqualTo(500);
        assertThat(response.base().componentsPerRescue()).isEqualTo(14);
        assertThat(response.base().foodPerRescue()).isEqualTo(12);
        assertThat(response.base().guardianLossPercent()).isEqualTo(35);
        assertThat(response.forecast()).isNull();
    }

    @Test
    @DisplayName("Closes each week on the base as it stood that Sunday")
    void shouldCloseEachWeekOnItsBase() {
        live();
        CampaignWeek first = CampaignFixtures.week(campaign, 1, 1_000, 50);
        first.setSettled(true);
        CampaignWeek second = CampaignFixtures.week(campaign, 2, 1_000, 50);
        CampaignWeek third = CampaignFixtures.week(campaign, 3, 1_000, 50);
        CampaignDailySnapshot saturday = snapshot(first.settlementDay().minusDays(1), 300, 900, 700);
        saturday.setFoodGained(400);
        saturday.setComponentsGained(300);
        CampaignDailySnapshot sunday = snapshot(first.settlementDay(), 500, 100, 40);
        sunday.setFoodGained(200);
        sunday.setComponentsGained(100);
        CampaignDailySnapshot wednesday = snapshot(TODAY, 1_000, 2_000, 1_400);
        wednesday.setFoodGained(50);
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of(first, second, third));
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(1L))
            .thenReturn(List.of(saturday, sunday, wednesday));

        CampaignResponse response = service.currentCampaign();

        assertThat(response.weeks().getFirst().base())
            .isEqualTo(new CampaignWeekBaseResponse(500, 500, 100, 40, 600, 400));
        assertThat(response.weeks().get(1).base())
            .isEqualTo(new CampaignWeekBaseResponse(1_000, 500, 2_000, 1_400, 50, 0));
        assertThat(response.weeks().get(2).base()).isNull();
    }

    @Test
    @DisplayName("Forecasts the Sunday of the week in progress from the base as it stands")
    void shouldForecastTheWeekInProgress() {
        live();
        CampaignWeek current = CampaignFixtures.week(campaign, 2, 1_000, 50);
        current.setDamageDealt(500);
        current.setChallengeRescued(10);
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of(current));
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(1L))
            .thenReturn(List.of(snapshot(TODAY, 1_000, 2_000, 1_400)));

        CampaignResponse response = service.currentCampaign();

        assertThat(response.forecast().weekIndex()).isEqualTo(2);
        assertThat(response.forecast().woundedCount()).isEqualTo(50);
        assertThat(response.forecast().challengeRescued()).isEqualTo(10);
        assertThat(response.forecast().extractionRescued()).isEqualTo(20);
        assertThat(response.forecast().rescued()).isEqualTo(30);
        assertThat(response.forecast().leftBehind()).isEqualTo(20);
        assertThat(response.forecast().limiter()).isEqualTo(ExtractionLimiter.GROUP);
        // A week still ahead of its Sunday has brought nobody home yet, whatever its challenges hold.
        assertThat(response.totals().rescued()).isZero();
    }

    @Test
    @DisplayName("Forecasts nothing once the week in progress is settled")
    void shouldNotForecastASettledWeek() {
        live();
        CampaignWeek current = CampaignFixtures.week(campaign, 2, 1_000, 50);
        current.setSettled(true);
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L)).thenReturn(List.of(current));
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(1L))
            .thenReturn(List.of(snapshot(TODAY, 1_000, 2_000, 1_400)));

        assertThat(service.currentCampaign().forecast()).isNull();
    }

    @Test
    @DisplayName("Reports how far each week got on its guardian")
    void shouldReportEachWeekProgress() {
        live();
        CampaignWeek fought = CampaignFixtures.week(campaign, 1, 1_000, 50);
        fought.setDamageDealt(700);
        fought.setLimiter(ExtractionLimiter.FOOD);
        fought.setBaseLoss(new BigDecimal("3.150"));
        fought.setSettled(true);
        CampaignWeek won = CampaignFixtures.week(campaign, 2, 1_000, 50);
        won.setDamageDealt(4_000);
        won.setDefeated(true);
        PlayerMatch finishing = new PlayerMatch();
        ValorantMatch match = new ValorantMatch();
        match.setMapName("Split");
        match.setGameMode(GameMode.COMPETITIVE);
        match.setRedScore(13);
        match.setBlueScore(11);
        finishing.setMatch(match);
        finishing.setTeamId("Red");
        finishing.setResult(MatchResult.WIN);
        finishing.setAgentName("Jett");
        won.setFinishingPlayerMatch(finishing);
        CampaignWeek untouched = CampaignFixtures.week(campaign, 3, 1_000, 50);

        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(1L))
            .thenReturn(List.of(fought, won, untouched));
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(1L)).thenReturn(List.of());

        CampaignResponse response = service.currentCampaign();

        assertThat(response.weeks())
            .extracting(CampaignWeekResponse::progressPercent)
            .containsExactly(70, 100, 0);
        assertThat(response.weeks().getFirst().limiter()).isEqualTo(ExtractionLimiter.FOOD);
        assertThat(response.weeks().getFirst().baseLoss()).isEqualTo(3);
        assertThat(response.weeks().getFirst().fatalBlow()).isNull();
        assertThat(response.weeks().get(1).fatalBlow())
            .isEqualTo(new FatalBlowResponse("Split", GameMode.COMPETITIVE, MatchResult.WIN, 13, 11, "Jett"));
        assertThat(response.totals().guardiansDefeated()).isEqualTo(1);
        assertThat(response.totals().weeksSettled()).isEqualTo(1);
    }

    @Test
    @DisplayName("Shows the last closed campaign once the live one is gone")
    void shouldFallBackOnTheLastClosedCampaign() {
        Campaign closed = CampaignFixtures.runningCampaign(2);
        closed.setStatus(CampaignStatus.CLOSED);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED))
            .thenReturn(List.of(closed));
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(2L)).thenReturn(List.of());
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(2L)).thenReturn(List.of());

        assertThat(service.currentCampaign().status()).isEqualTo(CampaignStatus.CLOSED);
    }

    @Test
    @DisplayName("Answers a null status rather than a 404 on a database without a campaign")
    void shouldAnswerWithoutACampaign() {
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED)).thenReturn(List.of());

        CampaignResponse response = service.currentCampaign();

        assertThat(response.status()).isNull();
        assertThat(response.today()).isEqualTo(TODAY);
        assertThat(response.weeks()).isEmpty();
        assertThat(response.base()).isNull();
    }

    @Test
    @DisplayName("Hands the day in progress to the day reader")
    void shouldDelegateTheDayInProgress() {
        live();
        CampaignTodayResponse expected = CampaignTodayResponse.none(TODAY);
        when(dayReader.read(campaign, TODAY, CampaignFixtures.FIRST_WEEK_START.plusWeeks(1)))
            .thenReturn(expected);

        assertThat(service.today()).isSameAs(expected);
    }

    @Test
    @DisplayName("Reports an empty day while no campaign is running")
    void shouldReportAnEmptyDayOutsideACampaign() {
        Campaign opened = CampaignFixtures.runningCampaign(1);
        opened.setStatus(CampaignStatus.OPENED);
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(opened));

        assertThat(service.today().players()).isEmpty();
        verifyNoInteractions(dayReader);
    }

    @Test
    @DisplayName("Lists the closed campaigns with their tier and their weekly curve")
    void shouldListTheClosedCampaigns() {
        Campaign closed = CampaignFixtures.runningCampaign(3);
        closed.setStatus(CampaignStatus.CLOSED);
        CampaignWeek settled = CampaignFixtures.week(closed, 1, 1_000, 50);
        settled.setSettled(true);
        settled.setDefeated(true);
        settled.setChallengeRescued(10);
        settled.setExtractionRescued(20);

        when(campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED))
            .thenReturn(List.of(closed));
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(3L)).thenReturn(List.of(settled));
        when(snapshotRepository.findAllByCampaignIdOrderByDayAsc(3L))
            .thenReturn(List.of(snapshot(settled.settlementDay(), 4_200, 0, 0)));

        List<CampaignHistoryResponse> history = service.history();

        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.guardiansDefeated()).isEqualTo(1);
            assertThat(entry.rescued()).isEqualTo(30);
            assertThat(entry.population()).isEqualTo(4_200);
            assertThat(entry.weeklyPopulation()).containsExactly(4_200);
        });
    }

    /**
     * Declares the fixture campaign as the live one.
     */
    private void live() {
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));
        lenient().when(campaignRepository.findAllByStatusOrderByNumberDesc(any())).thenReturn(List.of());
    }

    /**
     * Builds one stored day of the base.
     *
     * @param day        calendar day
     * @param population inhabitants
     * @param food       food in reserve
     * @param components components in reserve
     * @return the snapshot
     */
    private CampaignDailySnapshot snapshot(LocalDate day, int population, int food, int components) {
        CampaignDailySnapshot snapshot = new CampaignDailySnapshot();
        snapshot.setCampaign(campaign);
        snapshot.setDay(day);
        snapshot.setPopulation(BigDecimal.valueOf(population));
        snapshot.setFoodStock(BigDecimal.valueOf(food));
        snapshot.setComponentsStock(BigDecimal.valueOf(components));
        snapshot.setFamineLoss(BigDecimal.ZERO);
        snapshot.setGuardianLoss(BigDecimal.ZERO);
        snapshot.setDamage(0);
        snapshot.setFoodGained(0);
        snapshot.setComponentsGained(0);

        return snapshot;
    }
}
