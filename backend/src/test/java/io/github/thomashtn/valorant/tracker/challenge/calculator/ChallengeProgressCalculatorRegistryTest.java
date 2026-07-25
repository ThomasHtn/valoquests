package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.exception.UnsupportedChallengeProgressModeException;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests challenge calculator registration and selection.
 */
class ChallengeProgressCalculatorRegistryTest {

    /**
     * Verifies that a calculator can be retrieved by its supported mode.
     */
    @Test
    void shouldReturnCalculatorForSupportedMode() {
        ChallengeProgressCalculator calculator =
            createCalculator(ProgressMode.SUM);

        ChallengeProgressCalculatorRegistry registry =
            new ChallengeProgressCalculatorRegistry(
                List.of(calculator)
            );

        assertThat(registry.getCalculator(ProgressMode.SUM))
            .isSameAs(calculator);
    }

    /**
     * Verifies that an unsupported progress mode produces a clear exception.
     */
    @Test
    void shouldRejectUnsupportedProgressMode() {
        ChallengeProgressCalculatorRegistry registry =
            new ChallengeProgressCalculatorRegistry(
                List.of()
            );

        assertThatThrownBy(
            () -> registry.getCalculator(ProgressMode.RATIO)
        )
            .isInstanceOf(
                UnsupportedChallengeProgressModeException.class
            )
            .hasMessageContaining("RATIO");
    }

    /**
     * Verifies that two calculators cannot support the same progress mode.
     */
    @Test
    void shouldRejectDuplicateCalculatorRegistration() {
        ChallengeProgressCalculator firstCalculator =
            createCalculator(ProgressMode.SUM);

        ChallengeProgressCalculator secondCalculator =
            createCalculator(ProgressMode.SUM);

        assertThatThrownBy(
            () -> new ChallengeProgressCalculatorRegistry(
                List.of(
                    firstCalculator,
                    secondCalculator
                )
            )
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple challenge calculators")
            .hasMessageContaining("SUM");
    }

    /**
     * Creates a mocked calculator supporting the requested progress mode.
     *
     * @param progressMode supported progress mode
     * @return configured calculator
     */
    private ChallengeProgressCalculator createCalculator(
        ProgressMode progressMode
    ) {
        ChallengeProgressCalculator calculator =
            mock(ChallengeProgressCalculator.class);

        when(calculator.supportedMode()).thenReturn(progressMode);

        return calculator;
    }
}
