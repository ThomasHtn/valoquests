package io.github.thomashtn.valoquests.shared.exception;

/**
 * Signals that the caller supplied a value the API cannot accept.
 *
 * <p>Answered with 400 and its message returned as is; use {@link IllegalArgumentException} for broken
 * internal expectations, which stay a 500.
 */
public class InvalidRequestException extends RuntimeException {

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message written for the caller.
     *
     * @param message description of what the caller must correct
     */
    public InvalidRequestException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a message written for the caller and an underlying cause.
     *
     * @param message description of what the caller must correct
     * @param cause   underlying failure, kept for the logs and never returned to the caller
     */
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
