package io.github.thomashtn.valoquests.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the finalized ranking for one historical week.
 */
@Schema(description = "Finalized ranking for one week.")
public record RankingHistoryWeekResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    Instant finalizedAt,
    Long winnerPlayerId,
    List<FinalRankingEntryResponse> ranking
) {
    /**
     * Exposes one player's frozen result for a finalized week.
     *
     * <p>These values are a snapshot, not a live projection: a finalized week is immutable, so they
     * never move again even if the matches behind them are recalculated.
     *
     * @param position            final rank, starting at 1, {@code null} when the player is not
     *     competitive and therefore never ranked
     * @param playerId            internal player identifier
     * @param displayName         player name shown in the ranking
     * @param challengeDamage     challenge damage awarded for the week
     * @param completedChallenges challenges the player completed that week
     * @param matchDamage         damage dealt by valued matches that week
     * @param regularityBonus     regularity bonus awarded for that week
     * @param teamBonus           sum of per-challenge team bonuses earned that week
     * @param activeDays          number of distinct active days that week
     * @param totalDamage         total damage dealt to the boss that week
     */
    public record FinalRankingEntryResponse(

        Integer position,
        Long playerId,
        String displayName,
        int challengeDamage,
        int completedChallenges,
        int matchDamage,
        int regularityBonus,
        int teamBonus,
        int activeDays,
        int totalDamage
    ) {
    }

    /**
     * Creates an immutable historical ranking response.
     */
    public RankingHistoryWeekResponse {
        ranking = List.copyOf(ranking);
    }

}
