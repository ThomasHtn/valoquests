package io.github.thomashtn.valoquests.scoring;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Provides access to every registered {@link ScoringRuleset}, by version.
 *
 * <p>New weeks and boss encounters always resolve through {@link #current()}. Recalculating an
 * already-finalized week or encounter must instead resolve through {@link #forVersion(int)} using the
 * version persisted on that row, so a future ruleset addition never changes closed history.
 */
@Component
public final class ScoringRulesetRegistry {

    /**
     * Rulesets indexed by their version number.
     */
    private final Map<Integer, ScoringRuleset> rulesetsByVersion;

    /**
     * Highest version currently registered, used for every new week or encounter.
     */
    private final ScoringRuleset current;

    /**
     * Creates the registry from every ruleset bean registered in the Spring application context.
     *
     * @param availableRulesets available scoring rulesets
     */
    public ScoringRulesetRegistry(List<ScoringRuleset> availableRulesets) {
        this.rulesetsByVersion = buildRegistry(availableRulesets);
        this.current = rulesetsByVersion.values().stream()
            .max(Comparator.comparingInt(ScoringRuleset::version))
            .orElseThrow(() -> new IllegalStateException(
                "No ScoringRuleset is registered."
            ));
    }

    /**
     * Returns the ruleset to use for a week or boss encounter being created now.
     *
     * @return current, highest-versioned ruleset
     */
    public ScoringRuleset current() {
        return current;
    }

    /**
     * Returns the ruleset a given version resolves to.
     *
     * @param version ruleset version, typically read from a persisted week or boss encounter
     * @return matching ruleset
     * @throws IllegalArgumentException when no ruleset is registered for that version
     */
    public ScoringRuleset forVersion(int version) {
        ScoringRuleset ruleset = rulesetsByVersion.get(version);

        if (ruleset == null) {
            throw new IllegalArgumentException(
                "No ScoringRuleset registered for version " + version + "."
            );
        }

        return ruleset;
    }

    /**
     * Builds and validates the ruleset registry.
     *
     * @param availableRulesets available ruleset beans
     * @return validated registry indexed by version
     */
    private Map<Integer, ScoringRuleset> buildRegistry(List<ScoringRuleset> availableRulesets) {
        if (availableRulesets.isEmpty()) {
            throw new IllegalStateException("No ScoringRuleset is registered.");
        }

        return availableRulesets.stream()
            .collect(Collectors.toUnmodifiableMap(
                ScoringRuleset::version,
                ruleset -> ruleset,
                (first, second) -> {
                    throw new IllegalStateException(
                        "Multiple ScoringRuleset beans registered for version " + first.version() + "."
                    );
                }
            ));
    }
}
