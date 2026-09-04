package io.github.thomashtn.valoquests.ranking.dto;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the current weekly ranking, each player's progress on every challenge on the board, and
 * the week's honours as they stand.
 *
 * @param weekStart    Monday identifying the week
 * @param weekEnd      Sunday closing the week
 * @param today        day whose daily challenge is shown next to the pack
 * @param calculatedAt instant the ranking was last rebuilt, {@code null} before the first build
 * @param ranking      one entry per tracked player, archived ones aside, best week first
 */
@Schema(description = "Current weekly player ranking.")
public record CurrentRankingResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    LocalDate today,
    Instant calculatedAt,
    List<RankingEntryResponse> ranking
) {
    /**
     * Creates an immutable current-ranking response.
     */
    public CurrentRankingResponse {
        ranking = List.copyOf(ranking);
    }

    /**
     * Exposes one player's live standing in the current week.
     *
     * <p>An inactive player is listed with their validation counts and their progress, and nothing
     * else: they measure themselves against the squad without adding to it or taking a slot.
     *
     * @param position                 current rank, starting at 1, {@code null} when the player is
     *     not competitive and therefore never ranked
     * @param previousPosition         rank held before the latest rebuild, {@code null} when none
     * @param positionVariation        places gained since the previous rebuild, negative when lost
     * @param player                   identity shown next to the rank
     * @param guardianDamage           damage dealt to the guardian so far this week
     * @param food                     food share of that damage
     * @param components               components share of that damage
     * @param matchCount               valued matches played so far this week
     * @param activeDays               distinct days with at least one valued match
     * @param streakDays               longest run of consecutive played days reached this week
     * @param challengePoints          points of the challenges validated so far
     * @param completedChallenges      weekly challenges validated so far
     * @param totalChallenges          weekly challenges selected for the week
     * @param completedDailyChallenges daily challenges validated so far this week
     * @param totalPoints              guardian damage plus challenge points: the ranking key
     * @param titles                   honours the player holds as the week stands
     * @param challengeProgress        the player's line on every challenge the board shows
     */
    public record RankingEntryResponse(

        Integer position,
        Integer previousPosition,
        int positionVariation,
        PlayerRankingResponse player,
        int guardianDamage,
        int food,
        int components,
        int matchCount,
        int activeDays,
        int streakDays,
        int challengePoints,
        int completedChallenges,
        int totalChallenges,
        int completedDailyChallenges,
        int totalPoints,
        List<WeeklyTitle> titles,
        List<ChallengeProgressResponse> challengeProgress
    ) {
        /**
         * Creates an immutable ranking entry.
         */
        public RankingEntryResponse {
            titles = List.copyOf(titles);
            challengeProgress = List.copyOf(challengeProgress);
        }
    }

    /**
     * Exposes the player identity displayed alongside a rank.
     *
     * @param id              internal player identifier
     * @param displayName     player name shown in the ranking
     * @param portrait        player portrait URL
     * @param competitiveTier current competitive tier
     * @param rankRating      current rank rating, {@code null} when the player is unranked
     */
    public record PlayerRankingResponse(

        Long id,
        String displayName,
        String portrait,
        CompetitiveTier competitiveTier,
        Integer rankRating
    ) {
    }

    /**
     * Exposes one player's standing on one challenge of the board.
     *
     * @param id            selection identifier, the one the challenge board also uses
     * @param code          stable catalogue code
     * @param name          challenge name shown to players
     * @param cadence       whether the challenge covers the week or one day
     * @param difficulty    difficulty tier, {@code null} for a daily challenge
     * @param day           day a daily challenge covers, {@code null} for a weekly one
     * @param metric        metric the challenge measures
     * @param currentValue  value reached so far, zero when not evaluated yet
     * @param targetValue   value required to validate the challenge
     * @param unit          unit the values are expressed in, {@code null} for a composite challenge
     * @param completed     whether the target has been reached
     * @param rankingPoints points validating it earns in the weekly ranking
     */
    public record ChallengeProgressResponse(

        Long id,
        String code,
        String name,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        LocalDate day,
        String metric,
        BigDecimal currentValue,
        BigDecimal targetValue,
        String unit,
        boolean completed,
        int rankingPoints
    ) {
    }
}
