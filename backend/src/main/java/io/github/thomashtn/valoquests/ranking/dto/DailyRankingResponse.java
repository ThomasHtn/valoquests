package io.github.thomashtn.valoquests.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes one day's board: what every player of the roster brought in, and how that compares to the
 * day before.
 *
 * <p>A day is not a short week. Only match damage exists at this scale — challenge damage, the
 * regularity bonus and the team bonus are all settled on the week — so a day's figure is the one
 * number the scale can honestly carry, and {@link DailyRankingEntryResponse#damageVariation()} is what
 * turns it into an answer to "did we have a good evening?".
 *
 * <p>The turnout the two counts report is measured on the competing squad alone, the same players the
 * positions below are handed to. A deactivated player is still listed and still scored, but counting
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
     * <p>Unlike the weekly board, nothing here is persisted: the figures are read back off the stored
     * matches through the same barème the weekly ranking and the colony use, daily diminishing returns
     * included, so one evening is priced identically wherever it is shown.
     *
     * @param position            rank on the day, starting at 1, {@code null} when the player is not
     *     competitive and therefore never consumes a ranking slot
     * @param playerId            internal player identifier
     * @param displayName         player name shown in the ranking
     * @param portrait            relative path of the player portrait, or {@code null} when unknown
     * @param matchDamage         damage dealt by that day's valued matches, after diminishing returns
     * @param previousMatchDamage the same figure for {@link DailyRankingResponse#previousDay()}
     * @param damageVariation     {@code matchDamage} minus {@code previousMatchDamage}
     */
    public record DailyRankingEntryResponse(

        Integer position,
        Long playerId,
        String displayName,
        String portrait,
        int matchDamage,
        int previousMatchDamage,
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
