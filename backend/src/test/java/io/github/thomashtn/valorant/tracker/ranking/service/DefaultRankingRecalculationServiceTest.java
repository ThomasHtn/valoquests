package io.github.thomashtn.valorant.tracker.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.boss.service.WeekRulesetResolver;
import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valorant.tracker.scoring.ScoringRulesetV1;
import io.github.thomashtn.valorant.tracker.scoring.service.WeeklyMatchDamageAggregator;
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
        WeekRulesetResolver rulesetResolver = mock(WeekRulesetResolver.class);
        WeeklyMatchDamageAggregator matchDamageAggregator = mock(WeeklyMatchDamageAggregator.class);
        Clock clock = Clock.fixed(
            Instant.parse("2026-07-21T10:00:00Z"),
            ZoneOffset.UTC
        );

        when(rulesetResolver.resolve(any())).thenReturn(new ScoringRulesetV1());
        when(matchDamageAggregator.aggregate(any(), any(), any()))
            .thenReturn(new WeeklyMatchDamageAggregator.Aggregate(0, 0));

        service = new DefaultRankingRecalculationService(
            playerRepository,
            progressRepository,
            scoreRepository,
            rulesetResolver,
            matchDamageAggregator,
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
        WeeklyChallenge easyWeeklyChallenge = createWeeklyChallenge(10L, ChallengeDifficulty.EASY);
        WeeklyChallenge normalWeeklyChallenge = createWeeklyChallenge(20L, ChallengeDifficulty.NORMAL);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer, secondPlayer));
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of());
        when(
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                    WEEK_START
                )
        ).thenReturn(List.of(
            createProgress(firstPlayer, easyWeeklyChallenge, true),
            createProgress(secondPlayer, normalWeeklyChallenge, true),
            createProgress(secondPlayer, easyWeeklyChallenge, false)
        ));

        service.recalculateCurrentRanking();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WeeklyPlayerScore>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(scoreRepository).saveAll(captor.capture());
        List<WeeklyPlayerScore> scores = captor.getValue();

        // NORMAL (2500) outranks EASY (1500) under ScoringRulesetV1's challenge damage barème. Each
        // challenge is completed by exactly one player here, so the team bonus stays at zero and does
        // not interfere with this ordering assertion.
        assertThat(scores).hasSize(2);
        assertThat(scores.get(0).getPlayer()).isSameAs(secondPlayer);
        assertThat(scores.get(0).getChallengeDamage()).isEqualTo(2500);
        assertThat(scores.get(0).getTotalDamage()).isEqualTo(2500);
        assertThat(scores.get(0).getCompletedChallenges()).isEqualTo(1);
        assertThat(scores.get(0).getPosition()).isEqualTo(1);
        assertThat(scores.get(1).getPlayer()).isSameAs(firstPlayer);
        assertThat(scores.get(1).getChallengeDamage()).isEqualTo(1500);
        assertThat(scores.get(1).getTotalDamage()).isEqualTo(1500);
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

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
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

    /**
     * Verifies that an inactive player still gets a score built with their real completed-challenge
     * count, but with zero damage and no ranking slot, and does not shift an active player down.
     */
    @Test
    void shouldSkipRankingSlotForInactivePlayer() {
        Player proPlayer = createPlayer(1L, "Pro");
        proPlayer.setStatus(PlayerStatus.INACTIVE);
        Player competitivePlayer = createPlayer(2L, "Regular");

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(proPlayer, competitivePlayer));
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of());
        when(
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                    WEEK_START
                )
        ).thenReturn(List.of(
            createProgress(proPlayer, createWeeklyChallenge(10L, ChallengeDifficulty.NORMAL), true)
        ));

        service.recalculateCurrentRanking();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WeeklyPlayerScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(scoreRepository).saveAll(captor.capture());
        List<WeeklyPlayerScore> scores = captor.getValue();

        WeeklyPlayerScore proScore = scores.stream()
            .filter(score -> score.getPlayer() == proPlayer)
            .findFirst()
            .orElseThrow();
        WeeklyPlayerScore competitiveScore = scores.stream()
            .filter(score -> score.getPlayer() == competitivePlayer)
            .findFirst()
            .orElseThrow();

        assertThat(proScore.getTotalDamage()).isZero();
        assertThat(proScore.getCompletedChallenges()).isEqualTo(1);
        assertThat(proScore.getPosition()).isNull();
        assertThat(competitiveScore.getPosition()).isEqualTo(1);
    }

    /** Creates a player fixture. */
    private Player createPlayer(Long id, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        return player;
    }

    /** Creates a weekly challenge fixture with a distinct identifier and difficulty. */
    private WeeklyChallenge createWeeklyChallenge(long id, ChallengeDifficulty difficulty) {
        Challenge challenge = new Challenge();
        challenge.setDifficulty(difficulty);
        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setId(id);
        weeklyChallenge.setChallenge(challenge);
        return weeklyChallenge;
    }

    /** Creates one persisted progress fixture. */
    private PlayerChallengeProgress createProgress(
        Player player,
        WeeklyChallenge weeklyChallenge,
        boolean completed
    ) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCompleted(completed);
        return progress;
    }
}
