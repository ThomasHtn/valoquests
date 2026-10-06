package io.github.thomashtn.valoquests.campaign.model;

import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import java.time.LocalDate;

/**
 * One player's day inside a campaign, on its way to being stored.
 *
 * @param playerId internal player identifier
 * @param day      calendar day
 * @param output   what the player produced that day, both multipliers applied
 */
public record CampaignPlayerDayInput(long playerId, LocalDate day, PlayerDayOutput output) {
}
