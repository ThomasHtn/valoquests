package io.github.thomashtn.valoquests.boss.exception;

/**
 * Indicates that a boss cannot be drawn for a week.
 */
public class WeeklyBossSelectionException extends RuntimeException {

    /**
     * Creates a weekly boss selection exception.
     *
     * @param message contextual error message
     */
    public WeeklyBossSelectionException(String message) {
        super(message);
    }
}
