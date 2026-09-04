package io.github.thomashtn.valoquests.ranking.dto;

import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the finalized ranking for one historical week.
 *
 * @param weekStart      Monday identifying the week
 * @param weekEnd        Sunday closing the week
 * @param finalizedAt    instant the week was frozen
 * @param winnerPlayerId player who finished first, {@code null} when nobody was ranked
 * @param ranking        every ranked player, first place first
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
     * @param position                 final rank, starting at 1
     * @param playerId                 internal player identifier
     * @param displayName              player name shown in the ranking
     * @param guardianDamage           damage dealt to the guardian that week
     * @param challengePoints          points of the challenges validated that week
     * @param totalPoints              guardian damage plus challenge points
     * @param completedChallenges      weekly challenges validated that week
     * @param completedDailyChallenges daily challenges validated that week
     * @param activeDays               distinct days with at least one valued match
     * @param streakDays               longest run of consecutive played days reached that week
     * @param titles                   honours the player won that week
     */
    public record FinalRankingEntryResponse(

        Integer position,
        Long playerId,
        String displayName,
        int guardianDamage,
        int challengePoints,
        int totalPoints,
        int completedChallenges,
        int completedDailyChallenges,
        int activeDays,
        int streakDays,
        List<WeeklyTitle> titles
    ) {
        /**
         * Creates an immutable entry.
         */
        public FinalRankingEntryResponse {
            titles = List.copyOf(titles);
        }
    }

    /**
     * Creates an immutable historical ranking response.
     */
    public RankingHistoryWeekResponse {
        ranking = List.copyOf(ranking);
    }
}
