package io.github.thomashtn.valoquests.campaign.model;

/**
 * What Sunday's rescue cost and brought back, as the replay computed it.
 *
 * @param weekIndex         one-based position in the campaign
 * @param challengeRescued  wounded the week's challenges brought back, capped by the group
 * @param extractionRescued wounded the ship extracted, after the guardian progress
 * @param foodSpent         food spent settling those extracted
 * @param componentsSpent   components spent reaching them
 * @param limiter           what capped the extraction
 * @param baseLoss          inhabitants a surviving guardian killed
 */
public record CampaignWeekSettlement(
    int weekIndex,
    int challengeRescued,
    int extractionRescued,
    int foodSpent,
    int componentsSpent,
    ExtractionLimiter limiter,
    double baseLoss
) {
}
