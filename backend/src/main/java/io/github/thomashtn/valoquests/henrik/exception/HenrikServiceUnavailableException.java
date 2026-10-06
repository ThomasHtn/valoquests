package io.github.thomashtn.valoquests.henrik.exception;


/**
 * Indicates that HenrikDev or the underlying Riot service is temporarily
 * unavailable.
 */
public class HenrikServiceUnavailableException extends HenrikApiException {

    /**
     * Creates a retryable service-unavailable exception.
     *
     * @param message external error description
     */
    public HenrikServiceUnavailableException(String message) {
        super(message, true);
    }
}
