package io.github.thomashtn.valorant.tracker.match.model;

/**
 * Records how a match's {@link GameMode} was determined.
 *
 * <p>Priority, highest first: {@link #MANUALLY_CORRECTED}, {@link #PROVIDED}, {@link #INFERRED},
 * {@link #UNKNOWN}. A later synchronization may enrich a stored match, but only with a value from a
 * source of equal or higher priority: a manual correction is never replaced by a synchronization, and
 * an inferred value never downgrades one Henrik already provided outright.
 */
public enum GameModeSource {

    /**
     * Resolved directly from Henrik's canonical queue identifier.
     */
    PROVIDED(2),

    /**
     * Resolved from a fallback identifier, such as the queue display name or mode type, because the
     * canonical one was missing or blank.
     */
    INFERRED(1),

    /**
     * Set by an administrator, overriding whatever synchronization would otherwise resolve.
     */
    MANUALLY_CORRECTED(3),

    /**
     * No identifier resolved to a known mode.
     */
    UNKNOWN(0);

    private final int priority;

    GameModeSource(int priority) {
        this.priority = priority;
    }

    /**
     * Indicates whether this source may overwrite a value currently attributed to another source.
     *
     * @param other source of the currently stored value
     * @return {@code true} when this source's priority is equal to or higher than {@code other}'s
     */
    public boolean outranksOrEquals(GameModeSource other) {
        return this.priority >= other.priority;
    }
}
