package io.github.thomashtn.valoquests.campaign.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What one player produced on the day being shown.
 *
 * <p>Both multipliers are reported, not just applied. A rule that discourages marathon sessions
 * only discourages one if the player can see it coming, and a bonus for the days played this week
 * only rewards regularity if the counter is on screen.
 *
 * @param playerId           internal player identifier
 * @param gameName           player's Riot name
 * @param tagLine            player's Riot tag
 * @param damage             damage dealt, both multipliers applied
 * @param food               food produced
 * @param components         components produced
 * @param matchCount         valued matches played
 * @param reducedMatchCount  those the day's diminishing returns priced below full value
 * @param playedDays         days played this week up to this day
 * @param streakBonusPercent bonus every match of the day earned from the days played this week
 */
@Schema(description = "One player's output on the day shown.")
public record CampaignPlayerDayResponse(
    long playerId,
    String gameName,
    String tagLine,
    int damage,
    int food,
    int components,
    int matchCount,
    int reducedMatchCount,
    int playedDays,
    int streakBonusPercent
) {
}
