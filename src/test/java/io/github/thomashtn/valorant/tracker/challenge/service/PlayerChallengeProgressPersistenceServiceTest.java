package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests player challenge progress persistence.
 */
class PlayerChallengeProgressPersistenceServiceTest {

    /**
     * Fixed calculation timestamp used by tests.
     */
    private static final Instant CALCULATION_TIME =
        Instant.parse("2026-07-20T12:00:00Z");

    /**
     * Progress repository dependency.
     */
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Service under test.
     */
    private PlayerChallengeProgressPersistenceService service;

    /**
     * Creates the test dependencies before each test.
     */
    @BeforeEach
    void setUp() {
        progressRepository =
            mock(PlayerChallengeProgressRepository.class);

        Clock clock = Clock.fixed(
            CALCULATION_TIME,
            ZoneOffset.UTC
        );

        service = new PlayerChallengeProgressPersistenceService(
            progressRepository,
            clock
        );
    }

    /**
     * Verifies that missing completed progress is created.
     */
    @Test
    void shouldCreateCompletedProgress() {
        Player player = createPlayer(1L);
        WeeklyChallenge weeklyChallenge =
            createWeeklyChallenge(10L);

        ChallengeProgressResult result =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(100)
            );

        when(
            progressRepository.findByPlayerIdAndWeeklyChallengeId(
                1L,
                10L
            )
        ).thenReturn(Optional.empty());

        when(progressRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PlayerChallengeProgress savedProgress = service.save(
            player,
            weeklyChallenge,
            result
        );

        assertThat(savedProgress.getPlayer()).isSameAs(player);
        assertThat(savedProgress.getWeeklyChallenge())
            .isSameAs(weeklyChallenge);
        assertThat(savedProgress.getCurrentValue())
            .isEqualByComparingTo("120");
        assertThat(savedProgress.getTargetValue())
            .isEqualByComparingTo("100");
        assertThat(savedProgress.isCompleted()).isTrue();
        assertThat(savedProgress.getCompletedAt())
            .isEqualTo(CALCULATION_TIME);
        assertThat(savedProgress.getCalculatedAt())
            .isEqualTo(CALCULATION_TIME);

        verify(progressRepository).save(savedProgress);
    }

    /**
     * Verifies that the original completion timestamp is preserved.
     */
    @Test
    void shouldPreserveFirstCompletionTimestamp() {
        Player player = createPlayer(1L);
        WeeklyChallenge weeklyChallenge =
            createWeeklyChallenge(10L);

        Instant firstCompletionTime =
            Instant.parse("2026-07-19T18:00:00Z");

        PlayerChallengeProgress existingProgress =
            new PlayerChallengeProgress();

        existingProgress.setPlayer(player);
        existingProgress.setWeeklyChallenge(weeklyChallenge);
        existingProgress.setCompleted(true);
        existingProgress.setCompletedAt(firstCompletionTime);

        ChallengeProgressResult result =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(100)
            );

        when(
            progressRepository.findByPlayerIdAndWeeklyChallengeId(
                1L,
                10L
            )
        ).thenReturn(Optional.of(existingProgress));

        when(progressRepository.save(existingProgress))
            .thenReturn(existingProgress);

        PlayerChallengeProgress savedProgress = service.save(
            player,
            weeklyChallenge,
            result
        );

        assertThat(savedProgress.getCompletedAt())
            .isEqualTo(firstCompletionTime);
        assertThat(savedProgress.getCalculatedAt())
            .isEqualTo(CALCULATION_TIME);
    }

    /**
     * Creates a persisted player for testing.
     *
     * @param id player identifier
     * @return configured player
     */
    private Player createPlayer(Long id) {
        Player player = new Player();

        player.setId(id);
        player.setDisplayName("Test player");

        return player;
    }

    /**
     * Creates a persisted weekly challenge for testing.
     *
     * @param id weekly challenge identifier
     * @return configured weekly challenge
     */
    private WeeklyChallenge createWeeklyChallenge(Long id) {
        WeeklyChallenge weeklyChallenge =
            new WeeklyChallenge();

        weeklyChallenge.setId(id);

        return weeklyChallenge;
    }
}
