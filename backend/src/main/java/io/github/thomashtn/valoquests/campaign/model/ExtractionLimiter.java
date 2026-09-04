package io.github.thomashtn.valoquests.campaign.model;

/**
 * What capped Sunday's extraction, so the week can say why it did not bring everyone home.
 *
 * <p>Reported instead of left to be inferred from three numbers on screen: the answer decides what
 * the squad should do differently next week, and "the ship ran out of components" is an instruction
 * where "1 240 components, 980 food, 42 wounded" is a puzzle.
 */
public enum ExtractionLimiter {

    /**
     * Every wounded of the week made it aboard. Nothing was binding.
     */
    NONE,

    /**
     * The group itself ran out: the squad could have carried more than the planet held.
     */
    GROUP,

    /**
     * Food ran out first, the seven protected meals excluded.
     */
    FOOD,

    /**
     * Components ran out first: the ship could not reach the rest of the group.
     */
    COMPONENTS
}
