package io.github.thomashtn.valoquests.campaign.dto;

/**
 * What the campaign has amounted to so far, across every week it has played.
 *
 * @param guardiansDefeated guardians that fell, which is also the rocket's state
 * @param weeksSettled      weeks whose Sunday has been settled
 * @param rescued           wounded brought home, challenges and extractions together
 * @param challengeRescued  the share of those the challenges brought back
 * @param damage            damage the roster has dealt
 * @param foodGained        food the roster has produced
 * @param componentsGained  components the roster has produced
 * @param inhabitantsLost   inhabitants lost to guardians and to famine
 */
public record CampaignTotalsResponse(
    int guardiansDefeated,
    int weeksSettled,
    int rescued,
    int challengeRescued,
    long damage,
    long foodGained,
    long componentsGained,
    int inhabitantsLost
) {
}
