package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;

/**
 * What Sunday would bring home if the week ended on the base as it stands.
 *
 * <p>A forecast, not a promise: the base still eats every evening, the guardian may fall, and the
 * challenges may bring more home. What it states is the composition of the rescue as of now, so a
 * squad can see which of the three limits is the one to push.
 *
 * @param weekIndex         one-based week the forecast is about
 * @param woundedCount      wounded stranded on the planet
 * @param challengeRescued  wounded the challenges have already brought home, acquired whatever happens
 * @param extractionRescued wounded the ship would bring home at the current breakthrough
 * @param rescued           the two added up
 * @param leftBehind        wounded who would stay on the ground
 * @param limiter           what caps the extraction right now
 */
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
