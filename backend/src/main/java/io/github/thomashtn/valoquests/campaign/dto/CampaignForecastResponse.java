package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What Sunday would bring home if the week ended on the base as it stands.
 *
 * <p>A forecast, not a promise: meals, the guardian and challenges can still change the outcome.
 *
 * @param weekIndex         one-based week the forecast is about
 * @param woundedCount      wounded stranded on the planet
 * @param challengeRescued  wounded the challenges have already brought home, acquired whatever happens
 * @param extractionRescued wounded the ship would bring home at the current breakthrough
 * @param rescued           the two added up
 * @param leftBehind        wounded who would stay on the ground
 * @param limiter           what caps the extraction right now
 */
@Schema(description = "What Sunday would bring home if the week ended now.")
public record CampaignForecastResponse(
    int weekIndex,
    int woundedCount,
    int challengeRescued,
    int extractionRescued,
    int rescued,
    int leftBehind,
    ExtractionLimiter limiter
) {
}
