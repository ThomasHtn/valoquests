package io.github.thomashtn.valoquests.challenge.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.model.CalculatedProgress;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
     * Weekly pack draw dependency.
     */
    private WeeklyChallengeDrawService weeklyDrawService;

    /**
     * Daily challenge draw dependency.
     */
    private DailyChallengeDrawService dailyDrawService;

    /**
     * Challenge calculation dependency.
     */
    private ChallengeProgressCalculationService calculationService;

    /**
     * Progress writer dependency.
     */
    private PlayerChallengeProgressWriter progressWriter;

    /**
     * Listener standing for the ranking, told once the current week is rebuilt.
     */
    private CurrentWeekProgressListener progressListener;

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
        weeklyDrawService = mock(WeeklyChallengeDrawService.class);
        dailyDrawService = mock(DailyChallengeDrawService.class);
        calculationService = mock(ChallengeProgressCalculationService.class);
        progressWriter = mock(PlayerChallengeProgressWriter.class);
        progressListener = mock(CurrentWeekProgressListener.class);
        WeekCalendar weekCalendar = new WeekCalendar(
            Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        service = new DefaultChallengeRecalculationService(
            playerRepository,
            calculationService,
            progressWriter,
            progressListener,
            weeklyDrawService,
            dailyDrawService,
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
        ChallengeSelection weekly = createWeekly();
        ChallengeSelection daily = createDaily(TODAY);
        List<CalculatedProgress> calculated = List.of(
            new CalculatedProgress(weekly, result(50)),
            new CalculatedProgress(daily, result(10))
        );

        when(weeklyDrawService.selectWeekChallenges(WEEK_START)).thenReturn(List.of(weekly));
        when(dailyDrawService.selectDailyChallenge(TODAY)).thenReturn(daily);
        when(dailyDrawService.findDailyChallenges(WEEK_START, TODAY)).thenReturn(List.of(daily));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player));
        when(calculationService.calculateWeek(player, WEEK_START, List.of(weekly, daily)))
            .thenReturn(calculated);

        service.drawAndRecalculateCurrentWeek();

        verify(dailyDrawService).selectDailyChallenge(TODAY);
        verify(progressWriter).saveAll(player, calculated);
        verify(progressListener).currentWeekProgressRecalculated();
    }

    /**
     * Verifies that a past week is rebuilt from the selections it owns, without drawing anything
     * and without touching the ranking.
     */
    @Test
    void shouldRecalculateAPastWeekFromItsOwnSelections() {
        Player player = createPlayer();
        LocalDate pastWeek = WEEK_START.minusWeeks(1);
        ChallengeSelection weekly = createWeekly();
        weekly.setWeekStart(pastWeek);
        List<CalculatedProgress> calculated = List.of(new CalculatedProgress(weekly, result(1)));

        when(weeklyDrawService.findExistingWeekChallenges(pastWeek)).thenReturn(List.of(weekly));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player));
        when(calculationService.calculateWeek(player, pastWeek, List.of(weekly))).thenReturn(calculated);

        service.recalculateWeekProgress(pastWeek);

        verify(weeklyDrawService, never()).selectWeekChallenges(any());
        verify(dailyDrawService, never()).selectDailyChallenge(any());
        verify(progressWriter).saveAll(player, calculated);
        verify(progressListener, never()).currentWeekProgressRecalculated();
    }

    /**
     * Verifies that challenge calculations are skipped when no tracked player exists.
     */
    @Test
    void shouldSkipCalculationsWhenNoPlayerExists() {
        when(weeklyDrawService.selectWeekChallenges(WEEK_START)).thenReturn(List.of(createWeekly()));
        when(dailyDrawService.selectDailyChallenge(TODAY)).thenReturn(createDaily(TODAY));
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of());

        service.drawAndRecalculateCurrentWeek();

        verify(calculationService, never()).calculateWeek(any(Player.class), any(LocalDate.class), anyList());
        verify(progressWriter, never()).saveAll(any(Player.class), anyList());
        verify(progressListener).currentWeekProgressRecalculated();
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
     * Creates a weekly selection of the current week.
     *
     * @return weekly selection fixture
     */
    private ChallengeSelection createWeekly() {
        Challenge challenge = new Challenge();
        challenge.setId(20L);
        challenge.setCode("WEEKLY_CHALLENGE");

        ChallengeSelection selection = new ChallengeSelection();
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
    private ChallengeSelection createDaily(LocalDate day) {
        Challenge challenge = new Challenge();
        challenge.setId(21L);
        challenge.setCode("DAILY_CHALLENGE");
        challenge.setCadence(ChallengeCadence.DAILY);

        ChallengeSelection selection = new ChallengeSelection();
        selection.setId(11L);
        selection.setWeekStart(WEEK_START);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(day);
        selection.setChallenge(challenge);
        return selection;
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
