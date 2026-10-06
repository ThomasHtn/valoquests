package io.github.thomashtn.valoquests.challenge.exception;

/**
 * Indicates that a weekly pack or a daily challenge cannot be drawn.
 */
public class ChallengeDrawException
    extends RuntimeException {

    /**
     * Creates a challenge draw exception.
     *
     * @param message contextual error message
     */
    public ChallengeDrawException(
        String message
    ) {
        super(message);
    }
}
