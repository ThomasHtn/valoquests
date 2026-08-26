package io.github.thomashtn.valoquests.colony.model;

/**
 * What one closed week hands the colony on the Monday that settles it.
 *
 * <p>The two are read together because they come from the same event and are credited in the same
 * breath, but they behave nothing alike: materials are permanent and still standing on settlement day,
 * morale only sets the speed of the nights that follow and is repaired or ruined by the next fight.
 *
 * @param materials   materials the week's challenges and fight bring in
 * @param moraleDelta morale the fight moves, negative when the boss held
 */
public record ColonyWeekOutcome(int materials, double moraleDelta) {

    /**
     * A week that settled nothing: no challenge completed, no fight recorded.
     */
    public static final ColonyWeekOutcome NONE = new ColonyWeekOutcome(0, 0.0);
}
