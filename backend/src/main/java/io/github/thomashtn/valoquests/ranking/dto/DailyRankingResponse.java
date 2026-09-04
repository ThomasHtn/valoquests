package io.github.thomashtn.valoquests.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes one day's board: what every player of the roster brought in, how the day was priced, and
 * how that compares to the day before.
 *
 * <p>A day is not a short week. Only match output exists at this scale, the challenge points are
 * settled on the week, so a day's figure is the damage its matches dealt, split into the two
 * resources, and {@link DailyRankingEntryResponse#damageVariation()} is what turns it into an
 * answer to "did we have a good evening?".
 *
 * <p>The turnout the two counts report is measured on the competing squad alone, the same players the
 * positions below are handed to. A deactivated player is still listed and still priced, but counting
 * them here would put a presence over a board holding no slot for them.
 *
 * @param day               the day on the board, as an ISO-8601 date
 * @param previousDay       the day the variation is measured against
 * @param playedPlayerCount competing players who played at all that day
 * @param rosterPlayerCount competing players, deactivated and archived ones excluded
 * @param ranking           one entry per player of the roster, archived ones aside, best day first
 */
@Schema(description = "One day's ranking, and how it compares to the day before.")
public record DailyRankingResponse(

    LocalDate day,
    LocalDate previousDay,
    int playedPlayerCount,
    int rosterPlayerCount,
    List<DailyRankingEntryResponse> ranking
) {
    /**
     * Exposes one player's day.
     *
     * <p>Nothing here is persisted: the figures are read back off the stored matches through the same
     * reader the weekly ranking and the campaign use, both multipliers included, so one evening is
     * priced identically wherever it is shown. Both multipliers are reported, not just applied: a rule
     * that discourages marathon sessions only discourages one if the player can see it coming, and a
     * streak only rewards regularity if the counter is on screen.
     *
     * @param position           rank on the day, starting at 1, {@code null} when the player is not
     *     competitive and therefore never consumes a ranking slot
     * @param playerId           internal player identifier
     * @param displayName        player name shown in the ranking
     * @param portrait           relative path of the player portrait, or {@code null} when unknown
     * @param damage             damage dealt by that day's valued matches, both multipliers applied
     * @param food               food share of that damage
     * @param components         components share of that damage
     * @param matchCount         valued matches played that day
     * @param reducedMatchCount  those the day's diminishing returns priced below full value
     * @param streakDays         consecutive played days ending on that day, zero when not played
     * @param streakBonusPercent bonus every match of the day earned from that streak
     * @param streakAtStake      run of consecutive played days ending the day before: what playing
     *     today keeps alive, and what not playing loses
     * @param previousDamage     the damage figure for {@link DailyRankingResponse#previousDay()}
     * @param damageVariation    {@code damage} minus {@code previousDamage}
     */
    public record DailyRankingEntryResponse(

        Integer position,
        Long playerId,
        String displayName,
        String portrait,
        int damage,
        int food,
        int components,
        int matchCount,
        int reducedMatchCount,
        int streakDays,
        int streakBonusPercent,
        int streakAtStake,
        int previousDamage,
        int damageVariation
    ) {
    }

    /**
     * Creates an immutable daily ranking response.
     */
    public DailyRankingResponse {
        ranking = List.copyOf(ranking);
    }
}
