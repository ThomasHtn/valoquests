package io.github.thomashtn.valoquests.synchronization.service;

/**
 * Shapes error messages before they are stored on a synchronization row.
 *
 * <p>Every stored message goes through here: never blank, never longer than the column.
 */
final class SynchronizationErrorMessage {

    /**
     * Longest message the error column holds.
     */
    static final int MAXIMUM_LENGTH = 2_000;

    /**
     * Not instantiable: static helpers only.
     */
    private SynchronizationErrorMessage() {
    }

    /**
     * Reads an exception's message, falling back to its class name when it has none.
     *
     * @param exception failure to describe
     * @return a non-blank description
     */
    static String of(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }

    /**
     * Truncates a message, turning a blank one into {@code null} so a clean batch stores no message.
     *
     * @param message message to store, possibly blank
     * @return a storable message, or {@code null}
     */
    static String truncateOrNull(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        return message.length() <= MAXIMUM_LENGTH ? message : message.substring(0, MAXIMUM_LENGTH);
    }
}
