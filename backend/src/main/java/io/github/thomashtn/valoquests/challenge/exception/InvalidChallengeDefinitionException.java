package io.github.thomashtn.valoquests.challenge.exception;

/**
 * Raised when a persisted challenge contains an invalid or unsupported rule
 * definition.
 */
public class InvalidChallengeDefinitionException extends RuntimeException {

    /**
     * Creates an exception for an invalid challenge definition.
     *
     * @param message detailed validation message
     */
    public InvalidChallengeDefinitionException(String message) {
        super(message);
    }

    /**
     * Creates an exception for a challenge definition that could not be parsed.
     *
     * @param message detailed validation message
     * @param cause   parsing failure
     */
    public InvalidChallengeDefinitionException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
