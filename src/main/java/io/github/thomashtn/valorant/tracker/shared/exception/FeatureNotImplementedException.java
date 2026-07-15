package io.github.thomashtn.valorant.tracker.shared.exception;

/**
 * Indicates that an API contract exists but its business service has not yet been implemented.
 */
public class FeatureNotImplementedException extends RuntimeException {

    /**
     * Creates an exception for a missing application service.
     *
     * @param feature human-readable name of the unavailable feature
     */
    public FeatureNotImplementedException(String feature) {
        super(feature + " has not been implemented yet.");
    }
}
