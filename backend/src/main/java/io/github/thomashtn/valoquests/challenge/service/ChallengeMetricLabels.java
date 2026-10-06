package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import java.util.stream.Collectors;

/**
 * Builds the metric label the challenge endpoints expose.
 */
final class ChallengeMetricLabels {

    /**
     * Not instantiable: static helpers only.
     */
    private ChallengeMetricLabels() {
    }

    /**
     * Joins the distinct metric names of a definition, in declaration order.
     *
     * @param definition parsed definition
     * @return metric label
     */
    static String of(ChallengeDefinition definition) {
        return definition.conditions().stream()
            .map(condition -> condition.metric().name())
            .distinct()
            .collect(Collectors.joining(" + "));
    }
}
