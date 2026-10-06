package io.github.thomashtn.valoquests.henrik.exception;


/**
 * Indicates that Henrik could not find the requested Riot resource.
 */
public class HenrikResourceNotFoundException extends HenrikApiException {

    /**
     * Creates a non-retryable resource-not-found exception.
     *
     * @param message external error description
     */
    public HenrikResourceNotFoundException(String message) {
        super(message, false);
    }
}
