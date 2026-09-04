package io.github.thomashtn.valoquests.campaign.model;

/**
 * The shape one week of a campaign is played at, expressed in shares of the squad's reference.
 *
 * <p>Two numbers are enough to make ten weeks feel different without any rule changing: a big group
 * behind a weak guardian is a supply run, a small group behind a hard one is a siege.
 *
 * @param weekIndex       one-based position in the campaign
 * @param planetName      planet the wounded are stranded on
 * @param category        weight class the week's guardian is drawn from
 * @param guardianWeight  guardian hit points, in shares of reference × active players
 * @param groupWeight     wounded to evacuate, in shares of reference × active players
 */
public record CampaignWeekShape(
    int weekIndex,
    String planetName,
    GuardianCategory category,
    double guardianWeight,
    double groupWeight
) {
}
