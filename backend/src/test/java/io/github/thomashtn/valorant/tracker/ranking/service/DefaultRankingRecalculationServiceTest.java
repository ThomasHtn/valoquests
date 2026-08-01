package io.github.thomashtn.valorant.tracker.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests weekly ranking aggregation, ordering and previous-position handling.
 */
class DefaultRankingRecalculationServiceTest {

    /** Current week resolved from the fixed application clock. */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /** Player repository dependency. */
    private PlayerRepository playerRepository;

    /** Progress repository dependency. */
    private PlayerChallengeProgressRepository progressRepository;

    /** Score repository dependency. */
    private WeeklyPlayerScoreRepository scoreRepository;

    /** Service under test. */
    private DefaultRankingRecalculationService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        progressRepository = mock(PlayerChallengeProgressRepository.class);
        scoreRepository = mock(WeeklyPlayerScoreRepository.class);
        Clock clock = Clock.fixed(
            Instant.parse("2026-07-21T10:00:00Z"),
            ZoneOffset.UTC
        );
        service = new DefaultRankingRecalculationService(
            playerRepository,
            progressRepository,
            scoreRepository,
            clock,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies completed challenge aggregation and deterministic ranking order.
     */
    @Test
    void shouldAggregateCompletedChallengesAndOrderPlayers() {
        Player firstPlayer = createPlayer(1L, "First");
        Player secondPlayer = createPlayer(2L, "Second");
        Challenge hundredPoints = createChallenge(100);
        Challenge twoHundredPoints = createChallenge(200);

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of(firstPlayer, secondPlayer));
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of());
        when(
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                    WEEK_START
                )
        ).thenReturn(List.of(
            createProgress(firstPlayer, hundredPoints, true),
            createProgress(secondPlayer, twoHundredPoints, true),
            createProgress(secondPlayer, hundredPoints, false)
        ));

        service.recalculateCurrentRanking();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WeeklyPlayerScore>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(scoreRepository).saveAll(captor.capture());
        List<WeeklyPlayerScore> scores = captor.getValue();

        assertThat(scores).hasSize(2);
        assertThat(scores.get(0).getPlayer()).isSameAs(secondPlayer);
        assertThat(scores.get(0).getPoints()).isEqualTo(200);
        assertThat(scores.get(0).getCompletedChallenges()).isEqualTo(1);
        assertThat(scores.get(0).getPosition()).isEqualTo(1);
        assertThat(scores.get(1).getPlayer()).isSameAs(firstPlayer);
        assertThat(scores.get(1).getPoints()).isEqualTo(100);
        assertThat(scores.get(1).getPosition()).isEqualTo(2);
    }

    /**
     * Verifies that a player's former position is retained for variation.
     */
    @Test
    void shouldPreservePreviousPositionDuringRecalculation() {
        Player player = createPlayer(1L, "Player");
        WeeklyPlayerScore existing = new WeeklyPlayerScore();
        existing.setPlayer(player);
        existing.setWeekStart(WEEK_START);
        existing.setPosition(3);

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of(player));
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of(existing));
        when(
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                    WEEK_START
                )
        ).thenReturn(List.of());

        service.recalculateCurrentRanking();

        verify(scoreRepository).saveAll(anyList());
        assertThat(existing.getPreviousPosition()).isEqualTo(3);
        assertThat(existing.getPosition()).isEqualTo(1);
    }

    /** Creates a player fixture. */
    private Player createPlayer(Long id, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        return player;
    }

    /** Creates a challenge fixture. */
    private Challenge createChallenge(int points) {
        Challenge challenge = new Challenge();
        challenge.setPoints(points);
        return challenge;
    }

    /** Creates one persisted progress fixture. */
    private PlayerChallengeProgress createProgress(
        Player player,
        Challenge challenge,
        boolean completed
    ) {
        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setChallenge(challenge);
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCompleted(completed);
        return progress;
    }
}
