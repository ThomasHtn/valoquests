package io.github.thomashtn.valoquests.challenge.exception;

/**
 * Indicates that a complete weekly challenge pack cannot be created.
 */
public class WeeklyChallengeSelectionException
    extends RuntimeException {

    /**
     * Creates a weekly challenge selection exception.
     *
     * @param message contextual error message
     */
    public WeeklyChallengeSelectionException(
        String message
    ) {
        super(message);
    }
}
