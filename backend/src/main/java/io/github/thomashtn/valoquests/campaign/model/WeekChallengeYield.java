package io.github.thomashtn.valoquests.campaign.model;

import java.util.Map;

/**
 * What one week's challenges brought back, before the group caps it.
 *
 * <p>Per player as well as in total: the total is what boards the ship on Sunday, the breakdown is
 * what the weekly Scout title and the operator's own contribution block are read from.
 *
 * @param survivors           wounded the whole roster's validated challenges brought back
 * @param survivorsByPlayer   the same, per operator
 * @param completionsByPlayer challenges each operator validated, daily and weekly together
 */
public record WeekChallengeYield(
    int survivors,
    Map<Long, Integer> survivorsByPlayer,
    Map<Long, Integer> completionsByPlayer
) {

    /**
     * A week whose challenges nobody validated.
     */
    public static final WeekChallengeYield NONE = new WeekChallengeYield(0, Map.of(), Map.of());

    /**
     * Creates an immutable yield.
     */
    public WeekChallengeYield {
        survivorsByPlayer = Map.copyOf(survivorsByPlayer);
        completionsByPlayer = Map.copyOf(completionsByPlayer);
    }
}
