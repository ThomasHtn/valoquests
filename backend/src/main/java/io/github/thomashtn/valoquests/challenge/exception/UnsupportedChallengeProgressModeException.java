package io.github.thomashtn.valoquests.challenge.exception;

import io.github.thomashtn.valoquests.challenge.model.ProgressMode;

/**
 * Indicates that no calculator is available for a challenge progress mode.
 */
public class UnsupportedChallengeProgressModeException
    extends RuntimeException {

    /**
     * Creates an exception describing the unsupported progress mode.
     *
     * @param progressMode unsupported progress mode
     */
    public UnsupportedChallengeProgressModeException(
        ProgressMode progressMode
    ) {
        super(
            "No challenge progress calculator is registered for progress mode "
                + progressMode
                + "."
        );
    }
}
