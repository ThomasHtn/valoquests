package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.ChallengeCatalogueResponse;

/**
 * Defines read operations for the challenge catalogue, outside of any one week's draw.
 *
 * <p>Kept apart from {@link ChallengeQueryService}: the two answer different questions of the
 * challenge model (what a whole week draws and completes, against what one catalogue entry is
 * always worth), and folding this in would have pushed
 * {@link DefaultChallengeQueryService}'s own constructor past checkstyle's parameter-count limit.
 */
public interface ChallengeCatalogueQueryService {

    /**
     * Returns every enabled challenge, weekly tiers and daily pool, as it would be drawn this week.
     *
     * @return the enabled challenge catalogue
     */
    ChallengeCatalogueResponse findCatalogue();
}
