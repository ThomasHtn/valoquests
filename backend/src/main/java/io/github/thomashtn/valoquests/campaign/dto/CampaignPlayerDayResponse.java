package io.github.thomashtn.valoquests.campaign.dto;

/**
 * What one operator produced on the day being shown.
 *
 * <p>Both multipliers are reported, not just applied. A rule that discourages marathon sessions
 * only discourages one if the player can see it coming, and a streak only rewards regularity if the
 * counter is on screen.
 *
 * @param playerId           internal player identifier
 * @param gameName           operator's Riot name
 * @param tagLine            operator's Riot tag
 * @param damage             damage dealt, both multipliers applied
 * @param food               food produced
 * @param components         components produced
 * @param matchCount         valued matches played
 * @param reducedMatchCount  those the day's diminishing returns priced below full value
 * @param streakDays         consecutive played days ending on this day
 * @param streakBonusPercent bonus every match of the day earned from that streak
 */
public record CampaignPlayerDayResponse(
    long playerId,
    String gameName,
    String tagLine,
    int damage,
    int food,
    int components,
    int matchCount,
    int reducedMatchCount,
    int streakDays,
    int streakBonusPercent
) {
}
