package io.github.thomashtn.valorant.tracker.shared.exception;

/**
 * Indicates that a requested application resource could not be found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
