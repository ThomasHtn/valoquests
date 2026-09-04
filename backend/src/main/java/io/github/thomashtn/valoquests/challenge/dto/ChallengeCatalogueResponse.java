package io.github.thomashtn.valoquests.challenge.dto;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Exposes the full catalogue of enabled challenges, independent of any single draw.
 *
 * @param reference reference the targets and rewards below were resolved against
 * @param challenges every enabled challenge, weekly tiers and daily pool alike
 */
@Schema(description = "Every enabled challenge, weekly tiers and daily pool, outside of any one draw.")
public record ChallengeCatalogueResponse(

    int reference,
    List<ChallengeCatalogueEntry> challenges
) {
    /**
     * Exposes one catalogue entry, as it would be drawn this week.
     *
     * @param id              internal challenge identifier
     * @param code            stable catalogue code
     * @param name            challenge name shown to players
     * @param description     challenge description shown to players
     * @param cadence         whether the challenge covers a week or a day
     * @param difficulty      difficulty tier, {@code null} for a daily challenge
     * @param competitiveOnly whether only ranked matches count
     * @param metric          metric the challenge measures
     * @param targetValue     progress target resolved against the calibration in force, the base
     *                        target outside any campaign
     * @param survivors       survivors one player brings back by completing it this week
     * @param rankingPoints   points one player earns in the weekly ranking by completing it
     */
    public record ChallengeCatalogueEntry(

        Long id,
        String code,
        String name,
        String description,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        boolean competitiveOnly,
        String metric,
        BigDecimal targetValue,
        int survivors,
        int rankingPoints
    ) {
    }

    /**
     * Creates an immutable catalogue response.
     */
    public ChallengeCatalogueResponse {
        challenges = List.copyOf(challenges);
    }
}
