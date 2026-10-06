package io.github.thomashtn.valoquests.ranking.model;

/**
 * One of the four weekly honours, so recognition never concentrates on a single player.
 *
 * <p>Purely honorific. One per player, none for the champion or on a tie; declaration order is the
 * award order.
 */
public enum WeeklyTitle {

    /**
     * Most days played during the week.
     */
    REGULAR,

    /**
     * Most challenges validated over the week, daily and weekly together.
     */
    SCOUT,

    /**
     * Most food produced over the week.
     */
    QUARTERMASTER,

    /**
     * Most components produced over the week.
     */
    MECHANIC
}
