package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.exception.UnsupportedChallengeProgressModeException;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Provides access to challenge progress calculators according to their
 * supported progress mode.
 */
@Component
public class ChallengeProgressCalculatorRegistry {

    /**
     * Calculators indexed by their supported progress mode.
     */
    private final Map<ProgressMode, ChallengeProgressCalculator> calculators;

    /**
     * Creates the calculator registry from every calculator bean registered
     * in the Spring application context.
     *
     * @param availableCalculators available challenge calculators
     */
    public ChallengeProgressCalculatorRegistry(
        List<ChallengeProgressCalculator> availableCalculators
    ) {
        this.calculators = buildRegistry(availableCalculators);
    }

    /**
     * Returns the calculator supporting the requested progress mode.
     *
     * @param progressMode requested progress mode
     * @return matching calculator
     * @throws UnsupportedChallengeProgressModeException when no calculator
     *                                                   supports the mode
     */
    public ChallengeProgressCalculator getCalculator(
        ProgressMode progressMode
    ) {
        ChallengeProgressCalculator calculator = calculators.get(progressMode);

        if (calculator == null) {
            throw new UnsupportedChallengeProgressModeException(progressMode);
        }

        return calculator;
    }

    /**
     * Indicates whether a calculator is registered for the requested mode.
     *
     * @param progressMode progress mode to verify
     * @return {@code true} when the mode can currently be calculated
     */
    public boolean supports(
        ProgressMode progressMode
    ) {
        return progressMode != null
            && calculators.containsKey(progressMode);
    }

    /**
     * Builds and validates the calculator registry.
     *
     * @param availableCalculators available calculator beans
     * @return validated calculator registry
     */
    private Map<ProgressMode, ChallengeProgressCalculator> buildRegistry(
        List<ChallengeProgressCalculator> availableCalculators
    ) {
        Map<ProgressMode, ChallengeProgressCalculator> registry =
            new EnumMap<>(ProgressMode.class);

        for (ChallengeProgressCalculator calculator : availableCalculators) {
            ProgressMode supportedMode = calculator.supportedMode();

            ChallengeProgressCalculator previousCalculator =
                registry.putIfAbsent(
                    supportedMode,
                    calculator
                );

            if (previousCalculator != null) {
                throw new IllegalStateException(
                    "Multiple challenge calculators support progress mode "
                        + supportedMode
                        + ": "
                        + previousCalculator.getClass().getSimpleName()
                        + " and "
                        + calculator.getClass().getSimpleName()
                        + "."
                );
            }
        }

        return Map.copyOf(registry);
    }
}
