package io.github.thomashtn.valoquests.shared.exception;

/**
 * Indicates that a requested application resource could not be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a not-found exception with the supplied message.
     *
     * @param message error message exposed by the API
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
