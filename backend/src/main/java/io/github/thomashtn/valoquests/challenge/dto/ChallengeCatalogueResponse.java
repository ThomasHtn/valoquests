package io.github.thomashtn.valoquests.challenge.dto;

import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
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
     * @param tier      tier, {@code null} for a daily challenge
     * @param competitiveOnly whether only ranked matches count
     * @param metric          metric the challenge measures
     * @param targetValue     progress target of the grid the calibration in force selects
     * @param survivors       survivors one player brings back by completing it this week, also
     *                        the points it earns in the weekly ranking
     */
    public record ChallengeCatalogueEntry(

        Long id,
        String code,
        String name,
        String description,
        ChallengeCadence cadence,
        ChallengeTier tier,
        boolean competitiveOnly,
        String metric,
        BigDecimal targetValue,
        int survivors
    ) {
    }

    /**
     * Creates an immutable catalogue response.
     */
    public ChallengeCatalogueResponse {
        challenges = List.copyOf(challenges);
    }
}
