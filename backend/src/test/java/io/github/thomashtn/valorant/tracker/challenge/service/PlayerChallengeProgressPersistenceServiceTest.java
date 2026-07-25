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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
     * Verifies that several progress rows are loaded and saved in batches.
     */
    @Test
    void shouldSaveProgressInBatch() {
        Player player = createPlayer(1L);
        WeeklyChallenge firstChallenge = createWeeklyChallenge(10L);
        WeeklyChallenge secondChallenge = createWeeklyChallenge(11L);

        PlayerChallengeProgress existingProgress =
            new PlayerChallengeProgress();

        existingProgress.setPlayer(player);
        existingProgress.setWeeklyChallenge(firstChallenge);
        existingProgress.setCompleted(false);

        ChallengeProgressResult firstResult =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100)
            );

        ChallengeProgressResult secondResult =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(100)
            );

        when(
            progressRepository.findAllByPlayerIdAndWeeklyChallengeIdIn(
                1L,
                List.of(10L, 11L)
            )
        ).thenReturn(List.of(existingProgress));

        when(progressRepository.saveAll(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<PlayerChallengeProgress> savedProgress = service.saveAll(
            player,
            List.of(firstChallenge, secondChallenge),
            List.of(firstResult, secondResult)
        );

        assertThat(savedProgress).hasSize(2);
        assertThat(savedProgress.getFirst()).isSameAs(existingProgress);
        assertThat(savedProgress.getFirst().isCompleted()).isTrue();
        assertThat(savedProgress.getFirst().getCompletedAt())
            .isEqualTo(CALCULATION_TIME);

        PlayerChallengeProgress createdProgress = savedProgress.get(1);

        assertThat(createdProgress.getPlayer()).isSameAs(player);
        assertThat(createdProgress.getWeeklyChallenge())
            .isSameAs(secondChallenge);
        assertThat(createdProgress.getCurrentValue())
            .isEqualByComparingTo("25");
        assertThat(createdProgress.isCompleted()).isFalse();
        assertThat(createdProgress.getCompletedAt()).isNull();
        assertThat(createdProgress.getCalculatedAt())
            .isEqualTo(CALCULATION_TIME);

        verify(progressRepository)
            .findAllByPlayerIdAndWeeklyChallengeIdIn(
                1L,
                List.of(10L, 11L)
            );
        verify(progressRepository).saveAll(savedProgress);
        verifyNoMoreInteractions(progressRepository);
    }

    /**
     * Verifies that invalid batch sizes are rejected before repository access.
     */
    @Test
    void shouldRejectMismatchedBatchSizes() {
        Player player = createPlayer(1L);
        WeeklyChallenge weeklyChallenge = createWeeklyChallenge(10L);

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.saveAll(
                player,
                List.of(weeklyChallenge),
                List.of()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same size");

        verifyNoMoreInteractions(progressRepository);
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
