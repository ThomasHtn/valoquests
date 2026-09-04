package io.github.thomashtn.valoquests.campaign.exception;

import io.github.thomashtn.valoquests.shared.exception.ConflictException;

/**
 * Signals that a campaign cannot be opened, started or stopped as asked.
 *
 * <p>A conflict rather than a bad request: every case is a state the backoffice can see and fix —
 * a campaign already running, an empty roster, a guardian catalogue too small to draw from.
 */
public class CampaignLifecycleException extends ConflictException {

    /**
     * Creates the exception.
     *
     * @param message reason the operation was refused
     */
    public CampaignLifecycleException(String message) {
        super(message);
    }
}
