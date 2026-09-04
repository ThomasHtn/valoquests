package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.util.stream.Collectors;

/**
 * Builds the metric label the challenge endpoints expose.
 */
final class ChallengeMetricLabels {

    private ChallengeMetricLabels() {
    }

    /**
     * Joins the distinct metric names of a definition, in declaration order.
     *
     * <p>A progression challenge and an absolute one can share a metric while asking opposite
     * things, so the dormant baseline mode keeps its suffix: it is what would let the client tell
     * the two chips apart.
     *
     * @param definition parsed definition
     * @return metric label
     */
    static String of(ChallengeDefinition definition) {
        String suffix = definition.progressMode() == ProgressMode.BASELINE ? "_PROGRESS" : "";

        return definition.conditions().stream()
            .map(condition -> condition.metric().name() + suffix)
            .distinct()
            .collect(Collectors.joining(" + "));
    }
}
