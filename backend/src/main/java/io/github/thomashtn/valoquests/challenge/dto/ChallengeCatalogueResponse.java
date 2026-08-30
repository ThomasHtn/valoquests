package io.github.thomashtn.valoquests.challenge.dto;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Exposes the full catalogue of enabled challenges, independent of any single week's draw.
 */
@Schema(description = "Every challenge eligible for weekly selection, outside of any one week's draw.")
public record ChallengeCatalogueResponse(

    List<ChallengeCatalogueEntry> challenges
) {
    /**
     * Exposes one catalogue entry: what a challenge of this shape is always worth, regardless of
     * whether or how many players have cleared it this week.
     *
     * @param id          internal challenge identifier
     * @param name        challenge name shown to players
     * @param description challenge description shown to players
     * @param difficulty  difficulty tier controlling the reward
     * @param metric      metric the challenge measures
     * @param targetValue value a player must reach to complete it, or {@code null} for a
     *                    composite challenge with no single stored target
     * @param damage      base damage awarded on completion, before the squad bonus
     * @param materials   materials one player banks for the colony by validating it
     */
    public record ChallengeCatalogueEntry(

        Long id,
        String name,
        String description,
        ChallengeDifficulty difficulty,
        String metric,
        BigDecimal targetValue,
        int damage,
        int materials
    ) {
    }

    /**
     * Creates an immutable catalogue response.
     */
    public ChallengeCatalogueResponse {
        challenges = List.copyOf(challenges);
    }
}
