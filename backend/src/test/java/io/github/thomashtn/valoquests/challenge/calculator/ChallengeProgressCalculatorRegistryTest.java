package io.github.thomashtn.valoquests.challenge.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests challenge calculator registration and selection.
 */
class ChallengeProgressCalculatorRegistryTest {

    /**
     * Verifies that a calculator can be retrieved by its supported mode.
     */
    @Test
    @DisplayName("Returns the calculator registered for a progress mode")
    void shouldReturnCalculatorForSupportedMode() {
        List<ChallengeProgressCalculator> calculators = Arrays.stream(ProgressMode.values())
            .map(this::createCalculator)
            .toList();

        ChallengeProgressCalculatorRegistry registry =
            new ChallengeProgressCalculatorRegistry(calculators);

        assertThat(registry.getCalculator(ProgressMode.SUM))
            .isSameAs(calculators.get(ProgressMode.SUM.ordinal()));
    }

    /**
     * Verifies that a progress mode without a calculator stops the application at startup.
     */
    @Test
    @DisplayName("Refuses to start when a progress mode has no calculator")
    void shouldRejectAMissingCalculator() {
        assertThatThrownBy(
            () -> new ChallengeProgressCalculatorRegistry(
                List.of(createCalculator(ProgressMode.SUM))
            )
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No challenge calculator supports progress mode");
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
