package io.github.thomashtn.valorant.tracker.ranking.dto;

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
     * @param position            final rank, starting at 1
     * @param playerId            internal player identifier
     * @param displayName         player name shown in the ranking
     * @param points              points awarded for the week
     * @param completedChallenges challenges the player completed that week
     */
    public record FinalRankingEntryResponse(

        int position,
        Long playerId,
        String displayName,
        int points,
        int completedChallenges
    ) {
    }

    /**
     * Creates an immutable historical ranking response.
     */
    public RankingHistoryWeekResponse {
        ranking = List.copyOf(ranking);
    }

}
