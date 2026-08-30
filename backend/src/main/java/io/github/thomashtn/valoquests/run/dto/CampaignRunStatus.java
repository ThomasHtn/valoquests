package io.github.thomashtn.valoquests.run.dto;

/**
 * A run's own place in the campaign's lifecycle.
 */
public enum CampaignRunStatus {

    /**
     * The run in progress.
     */
    RUNNING,

    /**
     * Closed after running its full ten weeks and settlement day.
     */
    COMPLETED,

    /**
     * Closed early by an operator, its score frozen at the day it was stopped.
     */
    STOPPED
}
