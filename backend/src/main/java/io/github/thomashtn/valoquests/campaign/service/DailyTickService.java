package io.github.thomashtn.valoquests.campaign.service;

/**
 * Runs the daily tick shared by the nightly scheduler and the backoffice.
 */
public interface DailyTickService {

    /**
     * Draws the day's challenge, evaluates it, starts a due campaign and replays the campaign.
     *
     * <p>Every step is idempotent, so running it twice produces the very same rows.
     */
    void run();
}
