package io.github.thomashtn.valoquests.match.model;

/**
 * Records how a match's {@link GameMode} was determined.
 *
 * <p>Priority, highest first: {@link #MANUALLY_CORRECTED}, {@link #PROVIDED}, {@link #INFERRED},
 * {@link #UNKNOWN}. A stored value is only replaced by a source of equal or higher priority.
 */
public enum GameModeSource {

    /**
     * Resolved directly from Henrik's canonical queue identifier.
     */
    PROVIDED(2),

    /**
     * Resolved from a fallback identifier, such as the queue display name or mode type.
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

    /**
     * Trust order between sources: a higher value overrides a lower one.
     */
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
