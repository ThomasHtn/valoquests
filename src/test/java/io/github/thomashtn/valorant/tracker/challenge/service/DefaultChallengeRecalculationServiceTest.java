package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valorant.tracker.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valorant.tracker.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.service.RankingRecalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests current-week challenge progress orchestration.
 */
class DefaultChallengeRecalculationServiceTest {

    /**
     * Current Monday resolved from the fixed clock.
     */
    private static final LocalDate WEEK_START =
        LocalDate.of(2026, 7, 20);

    /**
     * Player repository dependency.
     */
    private PlayerRepository playerRepository;

    /**
     * Weekly challenge selection dependency.
     */
    private WeeklyChallengeSelectionService
        weeklyChallengeSelectionService;

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
    private PlayerChallengeProgressPersistenceService
        persistenceService;

    /**
     * Ranking recalculation dependency.
     */
    private RankingRecalculationService rankingRecalculationService;

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
        weeklyChallengeSelectionService =
            mock(WeeklyChallengeSelectionService.class);
        contextFactory =
            mock(PlayerChallengeContextFactory.class);
        calculationService =
            mock(ChallengeProgressCalculationService.class);
        persistenceService =
            mock(PlayerChallengeProgressPersistenceService.class);
        rankingRecalculationService =
            mock(RankingRecalculationService.class);

        Clock clock = Clock.fixed(
            Instant.parse("2026-07-20T12:00:00Z"),
            ZoneOffset.UTC
        );

        service = new DefaultChallengeRecalculationService(
            playerRepository,
            contextFactory,
            calculationService,
            persistenceService,
            rankingRecalculationService,
            weeklyChallengeSelectionService,
            clock
        );
    }

    /**
     * Verifies that weekly challenges are selected before every active player
     * and challenge is processed.
     */
    @Test
    void shouldSelectAndRecalculateCurrentWeekProgress() {
        Player player = createPlayer();
        Challenge challenge = createChallenge();

        WeeklyChallenge weeklyChallenge =
            createWeeklyChallenge(challenge);

        PlayerChallengeContext context =
            mock(PlayerChallengeContext.class);

        ChallengeProgressResult result =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(100)
            );

        when(
            weeklyChallengeSelectionService.selectWeekChallenges(
                WEEK_START
            )
        ).thenReturn(List.of(weeklyChallenge));

        when(
            playerRepository.findAllByStatusOrderByIdAsc(
                PlayerStatus.ACTIVE
            )
        ).thenReturn(List.of(player));

        when(contextFactory.create(player, WEEK_START))
            .thenReturn(context);

        when(context.playerMatches())
            .thenReturn(List.of());

        when(
            calculationService.calculate(
                challenge,
                context
            )
        ).thenReturn(result);

        service.recalculateCurrentWeekProgress();

        verify(
            weeklyChallengeSelectionService
        ).selectWeekChallenges(WEEK_START);

        verify(contextFactory).create(
            player,
            WEEK_START
        );

        verify(calculationService).calculate(
            challenge,
            context
        );

        verify(persistenceService).saveAll(
            player,
            List.of(weeklyChallenge),
            List.of(result)
        );

        verify(rankingRecalculationService)
            .recalculateCurrentRanking();
    }

    /**
     * Verifies that challenge calculations are skipped when no active player
     * exists.
     */
    @Test
    void shouldSkipCalculationsWhenNoActivePlayerExists() {
        Challenge challenge = createChallenge();

        WeeklyChallenge weeklyChallenge =
            createWeeklyChallenge(challenge);

        when(
            weeklyChallengeSelectionService.selectWeekChallenges(
                WEEK_START
            )
        ).thenReturn(List.of(weeklyChallenge));

        when(
            playerRepository.findAllByStatusOrderByIdAsc(
                PlayerStatus.ACTIVE
            )
        ).thenReturn(List.of());

        service.recalculateCurrentWeekProgress();

        verify(
            weeklyChallengeSelectionService
        ).selectWeekChallenges(WEEK_START);

        verify(
            contextFactory,
            never()
        ).create(
            org.mockito.ArgumentMatchers.any(Player.class),
            org.mockito.ArgumentMatchers.any(LocalDate.class)
        );

        verify(
            calculationService,
            never()
        ).calculate(
            org.mockito.ArgumentMatchers.any(Challenge.class),
            org.mockito.ArgumentMatchers.any(
                PlayerChallengeContext.class
            )
        );

        verify(
            persistenceService,
            never()
        ).saveAll(
            org.mockito.ArgumentMatchers.any(Player.class),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyList()
        );

        verify(rankingRecalculationService)
            .recalculateCurrentRanking();
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
     * Creates a persisted challenge.
     *
     * @return configured challenge
     */
    private Challenge createChallenge() {
        Challenge challenge = new Challenge();

        challenge.setId(20L);
        challenge.setCode("TEST_CHALLENGE");

        return challenge;
    }

    /**
     * Creates a current weekly challenge.
     *
     * @param challenge associated challenge
     * @return configured weekly challenge
     */
    private WeeklyChallenge createWeeklyChallenge(
        Challenge challenge
    ) {
        WeeklyChallenge weeklyChallenge =
            new WeeklyChallenge();

        weeklyChallenge.setId(10L);
        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);

        return weeklyChallenge;
    }
}
