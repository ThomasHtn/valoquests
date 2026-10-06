package io.github.thomashtn.valoquests.shared.exception;

/**
 * Signals that a request is well-formed but conflicts with the application's current state.
 *
 * <p>Unlike {@link InvalidRequestException}, the request itself is valid and may succeed if repeated later.
 */
public class ConflictException extends RuntimeException {

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message written for the caller.
     *
     * @param message description of the conflicting state
     */
    public ConflictException(String message) {
        super(message);
    }
}
