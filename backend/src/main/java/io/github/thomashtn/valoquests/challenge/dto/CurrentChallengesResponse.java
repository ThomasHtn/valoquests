package io.github.thomashtn.valoquests.challenge.dto;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes collective progress for the challenges selected for the current week.
 */
@Schema(description = "Current weekly challenges and their collective completion progress.")
public record CurrentChallengesResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    Instant lastSuccessfulSynchronizationAt,
    List<ChallengeProgressResponse> challenges
) {
    /**
     * Exposes one weekly challenge and how far the group has got with it.
     *
     * <p>Progress here is collective, not per player: it answers "how many of us finished this"
     * rather than "how far am I".
     *
     * @param id                   internal challenge identifier
     * @param name                 challenge name shown to players
     * @param description          challenge description shown to players
     * @param difficulty           difficulty tier the challenge was selected for
     * @param metric               metric the challenge measures
     * @param targetValue          value a player must reach to complete it
     * @param damage               base damage awarded on completion, before the squad bonus
     * @param materials            materials one player banks for the colony by validating it. The other
     *                             half of what a challenge is worth: the damage moves the weekly
     *                             ranking and the fight, the materials move the town, and nothing in the
     *                             interface said the second half existed.
     * @param teamBonusPercent     squad bonus currently earned, as a percentage of {@code damage}
     * @param completedPlayers     tracked players who completed it
     * @param totalPlayers         tracked players it applies to
     * @param completionPercentage completed players as a percentage of the total
     */
    public record ChallengeProgressResponse(

        Long id,
        String name,
        String description,
        ChallengeDifficulty difficulty,
        String metric,
        BigDecimal targetValue,
        int damage,
        int materials,
        int teamBonusPercent,
        int completedPlayers,
        int totalPlayers,
        BigDecimal completionPercentage
    ) {
    }

    /**
     * Creates an immutable current-challenges response.
     */
    public CurrentChallengesResponse {
        challenges = List.copyOf(challenges);
    }

}
