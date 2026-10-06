package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.model.CalculatedProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the player challenge progress writer.
 */
class PlayerChallengeProgressWriterTest {

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
    private PlayerChallengeProgressWriter writer;

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

        writer = new PlayerChallengeProgressWriter(
            progressRepository,
            clock
        );
    }

    /**
     * Verifies that the original completion timestamp is preserved.
     */
    @Test
    void shouldPreserveFirstCompletionTimestamp() {
        Player player = createPlayer(1L);
        ChallengeSelection selection =
            createSelection(10L);

        Instant firstCompletionTime =
            Instant.parse("2026-07-19T18:00:00Z");

        PlayerChallengeProgress existingProgress =
            new PlayerChallengeProgress();

        existingProgress.setPlayer(player);
        existingProgress.setSelection(selection);
        existingProgress.setCompleted(true);
        existingProgress.setCompletedAt(firstCompletionTime);

        ChallengeProgressResult result =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(150),
                BigDecimal.valueOf(100)
            );

        when(
            progressRepository.findAllByPlayerIdAndSelectionIdIn(
                1L,
                List.of(10L)
            )
        ).thenReturn(List.of(existingProgress));

        when(progressRepository.saveAll(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PlayerChallengeProgress savedProgress = writer.saveAll(
            player,
            List.of(new CalculatedProgress(selection, result))
        ).getFirst();

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
        ChallengeSelection firstChallenge = createSelection(10L);
        ChallengeSelection secondChallenge = createSelection(11L);

        PlayerChallengeProgress existingProgress =
            new PlayerChallengeProgress();

        existingProgress.setPlayer(player);
        existingProgress.setSelection(firstChallenge);
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
            progressRepository.findAllByPlayerIdAndSelectionIdIn(
                1L,
                List.of(10L, 11L)
            )
        ).thenReturn(List.of(existingProgress));

        when(progressRepository.saveAll(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<PlayerChallengeProgress> savedProgress = writer.saveAll(
            player,
            List.of(
                new CalculatedProgress(firstChallenge, firstResult),
                new CalculatedProgress(secondChallenge, secondResult)
            )
        );

        assertThat(savedProgress).hasSize(2);
        assertThat(savedProgress.getFirst()).isSameAs(existingProgress);
        assertThat(savedProgress.getFirst().isCompleted()).isTrue();
        assertThat(savedProgress.getFirst().getCompletedAt())
            .isEqualTo(CALCULATION_TIME);

        PlayerChallengeProgress createdProgress = savedProgress.get(1);

        assertThat(createdProgress.getPlayer()).isSameAs(player);
        assertThat(createdProgress.getSelection())
            .isSameAs(secondChallenge);
        assertThat(createdProgress.getCurrentValue())
            .isEqualByComparingTo("25");
        assertThat(createdProgress.isCompleted()).isFalse();
        assertThat(createdProgress.getCompletedAt()).isNull();
        assertThat(createdProgress.getCalculatedAt())
            .isEqualTo(CALCULATION_TIME);

        verify(progressRepository)
            .findAllByPlayerIdAndSelectionIdIn(
                1L,
                List.of(10L, 11L)
            );
        verify(progressRepository).saveAll(savedProgress);
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
    private ChallengeSelection createSelection(Long id) {
        ChallengeSelection selection =
            new ChallengeSelection();

        selection.setId(id);

        return selection;
    }
}
