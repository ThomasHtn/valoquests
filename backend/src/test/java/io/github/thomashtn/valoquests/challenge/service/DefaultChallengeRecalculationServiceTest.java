package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests current-week challenge progress orchestration, weekly pack and daily draws alike.
 */
class DefaultChallengeRecalculationServiceTest {

    /**
     * Current Monday resolved from the fixed clock.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Current day resolved from the fixed clock: the Wednesday.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    /**
     * Player repository dependency.
     */
    private PlayerRepository playerRepository;

    /**
     * Challenge selection dependency.
     */
    private WeeklyChallengeSelectionService selectionService;

    /**
     * Player context factory dependency.
     */
    private PlayerChallengeContextFactory contextFactory;

    /**
     * Challenge calculation dependency.
     */
    private ChallengeProgressCalculationService calculationService;

    /**
     * Progress persistence dependency.
     */
    private PlayerChallengeProgressPersistenceService persistenceService;

    /**
     * Ranking recalculation dependency.
     */
    private RankingRecalculationService rankingRecalculationService;

    /**
     * Calendar shared with the service.
     */
    private WeekCalendar weekCalendar;

    /**
     * Service under test.
     */
    private DefaultChallengeRecalculationService service;

    /**
     * Creates test dependencies before each test.
     */
    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        selectionService = mock(WeeklyChallengeSelectionService.class);
        contextFactory = mock(PlayerChallengeContextFactory.class);
        calculationService = mock(ChallengeProgressCalculationService.class);
        persistenceService = mock(PlayerChallengeProgressPersistenceService.class);
        rankingRecalculationService = mock(RankingRecalculationService.class);
        weekCalendar = new WeekCalendar(
            Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        service = new DefaultChallengeRecalculationService(
            playerRepository,
            contextFactory,
            calculationService,
            persistenceService,
            rankingRecalculationService,
            selectionService,
            weekCalendar
        );
    }

    /**
     * Verifies that the pack and today's challenge are drawn, then every selection of the week is
     * evaluated for every tracked player and the ranking rebuilt.
     */
    @Test
    void shouldSelectAndRecalculateCurrentWeekProgress() {
        Player player = createPlayer();
        WeeklyChallenge weekly = createWeekly();
        WeeklyChallenge daily = createDaily(TODAY);
        PlayerChallengeContext context = weekContext(player);
        ChallengeProgressResult weeklyResult = result(50);
        ChallengeProgressResult dailyResult = result(10);

        when(selectionService.selectWeekChallenges(WEEK_START)).thenReturn(List.of(weekly));
        when(selectionService.selectDailyChallenge(TODAY)).thenReturn(daily);
        when(selectionService.findDailyChallenges(WEEK_START, TODAY)).thenReturn(List.of(daily));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player));
        when(contextFactory.create(player, WEEK_START)).thenReturn(context);
        when(calculationService.calculate(eq(weekly), any())).thenReturn(weeklyResult);
        when(calculationService.calculate(eq(daily), any())).thenReturn(dailyResult);

        service.recalculateCurrentWeekProgress();

        verify(selectionService).selectDailyChallenge(TODAY);
        verify(calculationService).calculate(weekly, context);
        verify(persistenceService).saveAll(
            player,
            List.of(weekly, daily),
            List.of(weeklyResult, dailyResult)
        );
        verify(rankingRecalculationService).recalculateCurrentRanking();
    }

    /**
     * Verifies that a daily selection is evaluated over its own day only, carved out of the week.
     */
    @Test
    void shouldEvaluateADailyChallengeOverItsDayOnly() {
        Player player = createPlayer();
        WeeklyChallenge daily = createDaily(TODAY);
        PlayerMatch yesterday = matchAt(Instant.parse("2026-07-21T23:30:00Z"));
        PlayerMatch today = matchAt(Instant.parse("2026-07-22T09:00:00Z"));
        PlayerChallengeContext context = new PlayerChallengeContext(
            player.getId(),
            WEEK_START,
            weekCalendar.startOf(WEEK_START),
            weekCalendar.endOf(WEEK_START),
            List.of(yesterday, today)
        );

        when(selectionService.selectWeekChallenges(WEEK_START)).thenReturn(List.of());
        when(selectionService.selectDailyChallenge(TODAY)).thenReturn(daily);
        when(selectionService.findDailyChallenges(WEEK_START, TODAY)).thenReturn(List.of(daily));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player));
        when(contextFactory.create(player, WEEK_START)).thenReturn(context);
        when(calculationService.calculate(eq(daily), any())).thenReturn(result(1));

        service.recalculateCurrentWeekProgress();

        ArgumentCaptor<PlayerChallengeContext> captor =
            ArgumentCaptor.forClass(PlayerChallengeContext.class);
        verify(calculationService).calculate(eq(daily), captor.capture());

        PlayerChallengeContext dayContext = captor.getValue();
        assertThat(dayContext.periodStart()).isEqualTo(weekCalendar.startOfDay(TODAY));
        assertThat(dayContext.periodEnd()).isEqualTo(weekCalendar.endOfDay(TODAY));
        assertThat(dayContext.playerMatches()).containsExactly(today);
        assertThat(dayContext.weekStart()).isEqualTo(WEEK_START);
    }

    /**
     * Verifies that a past week is rebuilt from the selections it owns, without drawing anything
     * and without touching the ranking.
     */
    @Test
    void shouldRecalculateAPastWeekFromItsOwnSelections() {
        Player player = createPlayer();
        LocalDate pastWeek = WEEK_START.minusWeeks(1);
        WeeklyChallenge weekly = createWeekly();
        weekly.setWeekStart(pastWeek);
        PlayerChallengeContext context = weekContext(player);

        when(selectionService.findExistingWeekChallenges(pastWeek)).thenReturn(List.of(weekly));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player));
        when(contextFactory.create(player, pastWeek)).thenReturn(context);
        when(calculationService.calculate(eq(weekly), any())).thenReturn(result(1));

        service.recalculateWeekProgress(pastWeek);

        verify(selectionService, never()).selectWeekChallenges(any());
        verify(selectionService, never()).selectDailyChallenge(any());
        verify(persistenceService).saveAll(player, List.of(weekly), List.of(result(1)));
        verify(rankingRecalculationService, never()).recalculateCurrentRanking();
    }

    /**
     * Verifies that challenge calculations are skipped when no tracked player exists.
     */
    @Test
    void shouldSkipCalculationsWhenNoPlayerExists() {
        when(selectionService.selectWeekChallenges(WEEK_START)).thenReturn(List.of(createWeekly()));
        when(selectionService.selectDailyChallenge(TODAY)).thenReturn(createDaily(TODAY));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of());

        service.recalculateCurrentWeekProgress();

        verify(contextFactory, never()).create(any(Player.class), any(LocalDate.class));
        verify(persistenceService, never()).saveAll(any(Player.class), anyList(), anyList());
        verify(rankingRecalculationService).recalculateCurrentRanking();
    }

    /**
     * Creates an active persisted player.
     *
     * @return configured player
     */
    private Player createPlayer() {
        Player player = new Player();
        player.setId(1L);
        player.setDisplayName("Psilonnix");
        player.setStatus(PlayerStatus.ACTIVE);
        return player;
    }

    /**
     * Creates an empty context spanning the current week.
     *
     * @param player context owner
     * @return weekly context
     */
    private PlayerChallengeContext weekContext(Player player) {
        return new PlayerChallengeContext(
            player.getId(),
            WEEK_START,
            weekCalendar.startOf(WEEK_START),
            weekCalendar.endOf(WEEK_START),
            List.of()
        );
    }

    /**
     * Creates a weekly selection of the current week.
     *
     * @return weekly selection fixture
     */
    private WeeklyChallenge createWeekly() {
        Challenge challenge = new Challenge();
        challenge.setId(20L);
        challenge.setCode("WEEKLY_CHALLENGE");

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setId(10L);
        selection.setWeekStart(WEEK_START);
        selection.setChallenge(challenge);
        return selection;
    }

    /**
     * Creates a daily selection of the current week.
     *
     * @param day covered day
     * @return daily selection fixture
     */
    private WeeklyChallenge createDaily(LocalDate day) {
        Challenge challenge = new Challenge();
        challenge.setId(21L);
        challenge.setCode("DAILY_CHALLENGE");
        challenge.setCadence(ChallengeCadence.DAILY);

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setId(11L);
        selection.setWeekStart(WEEK_START);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(day);
        selection.setChallenge(challenge);
        return selection;
    }

    /**
     * Creates a player match started at one instant.
     *
     * @param startedAt match start
     * @return player match fixture
     */
    private PlayerMatch matchAt(Instant startedAt) {
        ValorantMatch match = new ValorantMatch();
        match.setStartedAt(startedAt);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        return playerMatch;
    }

    /**
     * Creates a result out of one hundred.
     *
     * @param current current value
     * @return progress result
     */
    private ChallengeProgressResult result(int current) {
        return ChallengeProgressResult.from(BigDecimal.valueOf(current), BigDecimal.valueOf(100));
    }
}
