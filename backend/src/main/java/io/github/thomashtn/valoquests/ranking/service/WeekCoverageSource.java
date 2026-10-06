package io.github.thomashtn.valoquests.ranking.service;

import java.time.LocalDate;

/**
 * Tells whether a week was played inside a campaign, the condition for crowning a champion.
 *
 * <p>The campaign package implements it: campaigns sit above the ranking, which cannot read them.
 */
public interface WeekCoverageSource {

    /**
     * Tells whether a campaign, running or already closed, covered one week.
     *
     * @param weekStart Monday identifying the week
     * @return {@code true} when a campaign covered it
     */
    boolean coveredByCampaign(LocalDate weekStart);
}
