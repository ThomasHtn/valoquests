package io.github.thomashtn.valoquests.challenge.dto;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes collective progress for the challenges of the current week: the weekly pack and every
 * daily challenge drawn so far this week.
 *
 * @param weekStart                       Monday of the current week
 * @param weekEnd                         Sunday of the current week
 * @param today                           current day, the one whose daily challenge is in play
 * @param lastSuccessfulSynchronizationAt last time progress was refreshed from Riot data
 * @param roster                          active players the challenges apply to, in roster order
 * @param challenges                      weekly pack, easiest tier first
 * @param dailies                         daily challenges drawn this week, oldest day first
 */
@Schema(description = "Current weekly challenges, this week's daily draws, and their collective completion.")
public record CurrentChallengesResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    LocalDate today,
    Instant lastSuccessfulSynchronizationAt,
    List<RosterPlayerResponse> roster,
    List<ChallengeProgressResponse> challenges,
    List<ChallengeProgressResponse> dailies
) {
    /**
     * Exposes one active player, the unit every completion count below is read against.
     *
     * @param id          player identifier, the one {@link ChallengeProgressResponse#completedPlayerIds()}
     *                    references
     * @param displayName name shown for the player
     */
    public record RosterPlayerResponse(

        Long id,
        String displayName
    ) {
    }

    /**
     * Exposes one selected challenge and how far the squad has got with it.
     *
     * <p>Progress here is collective, not per player: it answers "how many of us finished this"
     * rather than "how far am I".
     *
     * @param id                   selection identifier, the one progress rows reference
     * @param code                 stable catalogue code
     * @param name                 challenge name shown to players
     * @param description          challenge description shown to players
     * @param cadence              whether the challenge covers the week or one day
     * @param difficulty           difficulty tier, {@code null} for a daily challenge
     * @param day                  day a daily challenge covers, {@code null} for a weekly one
     * @param competitiveOnly      whether only ranked matches count
     * @param metric               metric the challenge measures
     * @param targetValue          value a player's progress must reach to complete it, resolved
     *                             against the campaign in force at draw time
     * @param survivors            survivors one player brings back by completing it, before the
     *                             weekly progression
     * @param rankingPoints        points one player earns in the weekly ranking by completing it
     * @param completedPlayers     active players who completed it
     * @param totalPlayers         active players it applies to
     * @param completedPlayerIds   identifiers of the active players who completed it, ascending
     * @param completionPercentage completed players as a percentage of the total
     */
    public record ChallengeProgressResponse(

        Long id,
        String code,
        String name,
        String description,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        LocalDate day,
        boolean competitiveOnly,
        String metric,
        BigDecimal targetValue,
        int survivors,
        int rankingPoints,
        int completedPlayers,
        int totalPlayers,
        List<Long> completedPlayerIds,
        BigDecimal completionPercentage
    ) {
        /**
         * Creates an immutable challenge progress response.
         */
        public ChallengeProgressResponse {
            completedPlayerIds = List.copyOf(completedPlayerIds);
        }
    }

    /**
     * Creates an immutable current-challenges response.
     */
    public CurrentChallengesResponse {
        roster = List.copyOf(roster);
        challenges = List.copyOf(challenges);
        dailies = List.copyOf(dailies);
    }
}
