package io.github.thomashtn.valoquests.scoring.model;

/**
 * Where a player stands on one day's diminishing-returns ladder, before their next match.
 *
 * <p>The ladder is applied retroactively everywhere else: a match is priced once it exists. This
 * says what the next one <em>will</em> be worth, which is the only form in which a rule meant to
 * discourage marathon sessions can actually discourage one.
 *
 * @param matchesToday     valued matches already played that day
 * @param nextMatchPercent share of its base damage the next match would keep
 * @param dropsAtRank      rank at which the share falls below {@link #nextMatchPercent}, or
 *     {@code null} once the ladder has reached its floor and nothing falls further
 * @param dropsToPercent   share kept from {@link #dropsAtRank} on, or {@code null} at the floor
 */
public record DailyYield(
    int matchesToday,
    int nextMatchPercent,
    Integer dropsAtRank,
    Integer dropsToPercent
) {
}
