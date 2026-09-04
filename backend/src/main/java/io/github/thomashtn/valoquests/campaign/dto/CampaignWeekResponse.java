package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One week of the campaign's map: its planet, its guardian and how its Sunday went.
 *
 * <p>A week the campaign has not reached yet is present with its target figures and zeroes for
 * everything else. The map is meant to be read ahead — a week ten with the biggest group behind the
 * biggest guardian is only a plan if it can be seen from week one.
 *
 * @param weekIndex          one-based position in the campaign
 * @param weekStart          Monday identifying the week
 * @param planetName         planet the wounded are stranded on
 * @param category           weight class of the week's guardian
 * @param guardianName       guardian's name
 * @param guardianDescription guardian's one-line description
 * @param guardianHitPoints  hit points the guardian opened the week with
 * @param damageDealt        damage the roster dealt over the week
 * @param progressPercent    share of the guardian's hit points removed, capped at a hundred
 * @param defeated           whether the guardian fell
 * @param defeatedAt         start instant of the match that landed the finishing blow
 * @param defeatedByPlayerId operator who landed it
 * @param woundedCount       wounded stranded on the planet
 * @param challengeRescued   wounded the week's challenges brought back
 * @param extractionRescued  wounded the ship extracted
 * @param foodSpent          food spent settling them
 * @param componentsSpent    components spent reaching them
 * @param limiter            what capped the extraction
 * @param baseLoss           inhabitants a surviving guardian killed
 * @param settled            whether the week's Sunday has been settled
 */
public record CampaignWeekResponse(
    int weekIndex,
    LocalDate weekStart,
    String planetName,
    GuardianCategory category,
    String guardianName,
    String guardianDescription,
    int guardianHitPoints,
    int damageDealt,
    int progressPercent,
    boolean defeated,
    Instant defeatedAt,
    Long defeatedByPlayerId,
    int woundedCount,
    int challengeRescued,
    int extractionRescued,
    int foodSpent,
    int componentsSpent,
    ExtractionLimiter limiter,
    int baseLoss,
    boolean settled
) {
}
