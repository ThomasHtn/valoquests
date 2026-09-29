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
     * @param portrait    agent portrait chosen by the player, {@code null} when none was chosen
     */
    public record RosterPlayerResponse(

        Long id,
        String displayName,
        String portrait
    ) {
    }

    /**
     * Exposes where one active player stands on one challenge.
     *
     * @param playerId     player identifier, one of the roster's
     * @param currentValue progress so far, zero while the player has not been evaluated on it
     * @param completed    whether the player completed it
     */
    public record PlayerProgressResponse(

        Long playerId,
        BigDecimal currentValue,
        boolean completed
    ) {
    }

    /**
     * Exposes one selected challenge and how far the squad has got with it.
     *
     * <p>Progress is read both ways: collectively ("how many of us finished this") and player by
     * player ("how far is each of us"), past days' challenges included.
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
     * @param players              each active player's progress, in roster order
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
        BigDecimal completionPercentage,
        List<PlayerProgressResponse> players
    ) {
        /**
         * Creates an immutable challenge progress response.
         */
        public ChallengeProgressResponse {
            completedPlayerIds = List.copyOf(completedPlayerIds);
            players = List.copyOf(players);
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
