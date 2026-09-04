package io.github.thomashtn.valoquests.campaign.model;

/**
 * One of the four weekly honours, so recognition never concentrates on a single operator.
 *
 * <p>Purely honorific: no title touches a score, a resource or a guardian. One operator can hold
 * several in the same week, and a tie awards nothing — a title shared is a title that says nothing.
 */
public enum WeeklyTitle {

    /**
     * Most components produced over the week.
     */
    MECHANIC,

    /**
     * Most food produced over the week.
     */
    QUARTERMASTER,

    /**
     * Longest run of consecutive played days reached during the week.
     */
    REGULAR,

    /**
     * Most challenges validated over the week, daily and weekly together.
     */
    SCOUT
}
