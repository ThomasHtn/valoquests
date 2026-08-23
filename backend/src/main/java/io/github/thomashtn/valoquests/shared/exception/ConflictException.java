package io.github.thomashtn.valoquests.shared.exception;

/**
 * Signals that a request is well-formed but conflicts with the application's current state.
 *
 * <p>Distinct from {@link InvalidRequestException}: nothing is wrong with what the caller sent, and
 * repeating the very same request later may well succeed. Typical cases are an operation refused
 * because another one is already running, and a creation refused because the entity already exists.
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
