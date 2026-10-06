package io.github.thomashtn.valoquests.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes one day's board: what every player of the roster brought in and how the day was priced.
 *
 * <p>Only match output exists at this scale, challenge points being settled on the week. The roster
 * count covers the competing squad only, the players positions go to.
 *
 * @param day               the day on the board, as an ISO-8601 date
 * @param rosterPlayerCount competing players, deactivated and archived ones excluded
 * @param ranking           one entry per player of the roster, archived ones aside, best day first
 */
@Schema(description = "One day's ranking.")
public record DailyRankingResponse(

    LocalDate day,
    int rosterPlayerCount,
    List<DailyRankingEntryResponse> ranking
) {
    /**
     * Exposes one player's day.
     *
     * <p>Both multipliers are reported, not just applied, so players see the diminishing returns and the
     * streak bonus coming.
     *
     * @param position           rank on the day, starting at 1, shared on equal damage, {@code null}
     *     when the player dealt none or is not competitive
     * @param competing          whether the player takes part in the ranking at all
     * @param playerId           internal player identifier
     * @param displayName        player name shown in the ranking
     * @param portrait           relative path of the player portrait, or {@code null} when unknown
     * @param damage             damage dealt by that day's valued matches, both multipliers applied
     * @param food               food share of that damage
     * @param components         components share of that damage
     * @param matchCount         valued matches played that day
     * @param reducedMatchCount  those the day's diminishing returns priced below full value
     * @param playedDays         days played this week up to that day included, zero when not played
     * @param streakBonusPercent bonus every match of the day earned from the days played this week
     * @param weekPlayedDays     days of the week played from Monday up to that day included
     */
    public record DailyRankingEntryResponse(

        Integer position,
        boolean competing,
        Long playerId,
        String displayName,
        String portrait,
        int damage,
        int food,
        int components,
        int matchCount,
        int reducedMatchCount,
        int playedDays,
        int streakBonusPercent,
        List<LocalDate> weekPlayedDays
    ) {

        /**
         * Creates an immutable entry.
         */
        public DailyRankingEntryResponse {
            weekPlayedDays = List.copyOf(weekPlayedDays);
        }
    }

    /**
     * Creates an immutable daily ranking response.
     */
    public DailyRankingResponse {
        ranking = List.copyOf(ranking);
    }
}
