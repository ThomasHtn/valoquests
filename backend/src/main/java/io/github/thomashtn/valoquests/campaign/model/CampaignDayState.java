package io.github.thomashtn.valoquests.campaign.model;

import java.time.LocalDate;

/**
 * The base at the close of one day, as the replay computed it.
 *
 * <p>Stocks and population are the state carried into the next day; everything else is what moved
 * that day, kept so a screen can explain a number rather than only show it.
 *
 * @param day              calendar day
 * @param damage           damage the roster dealt that day
 * @param foodGained       food produced that day
 * @param componentsGained components produced that day
 * @param growth           inhabitants the day's damage added
 * @param eaten            food the base consumed that evening
 * @param famineLoss       inhabitants lost to an empty larder
 * @param guardianLoss     inhabitants lost to a guardian left standing, Sundays only
 * @param arrivals         wounded who joined the base, Sundays only
 * @param foodStock        food in reserve at the close of the day
 * @param componentsStock  components in reserve at the close of the day
 * @param population       inhabitants at the close of the day
 * @param presenceCount    roster operators who played that day
 */
public record CampaignDayState(
    LocalDate day,
    int damage,
    int foodGained,
    int componentsGained,
    double growth,
    double eaten,
    double famineLoss,
    double guardianLoss,
    int arrivals,
    double foodStock,
    double componentsStock,
    double population,
    int presenceCount
) {
}
