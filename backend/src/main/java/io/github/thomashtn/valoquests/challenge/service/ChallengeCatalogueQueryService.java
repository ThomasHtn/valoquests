package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.ChallengeCatalogueResponse;

/**
 * Defines read operations for the challenge catalogue, outside of any one week's draw.
 *
 * <p>Kept apart from {@link ChallengeQueryService}, which answers what a week draws and completes.
 */
public interface ChallengeCatalogueQueryService {

    /**
     * Returns every enabled challenge, weekly tiers and daily pool, as it would be drawn this week.
     *
     * @return the enabled challenge catalogue
     */
    ChallengeCatalogueResponse findCatalogue();
}
