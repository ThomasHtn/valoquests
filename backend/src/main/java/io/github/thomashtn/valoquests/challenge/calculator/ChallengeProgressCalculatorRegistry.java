package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Provides access to challenge progress calculators according to their
 * supported progress mode.
 */
@Component
public final class ChallengeProgressCalculatorRegistry {

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
     * @return matching calculator, always present since the registry covers every mode
     */
    public ChallengeProgressCalculator getCalculator(
        ProgressMode progressMode
    ) {
        return calculators.get(progressMode);
    }

    /**
     * Builds the calculator registry, one calculator per progress mode, no more and no less.
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

        // Failing at startup beats discovering a missing calculator on the first draw.
        for (ProgressMode mode : ProgressMode.values()) {
            if (!registry.containsKey(mode)) {
                throw new IllegalStateException("No challenge calculator supports progress mode " + mode + ".");
            }
        }

        return Map.copyOf(registry);
    }
}
