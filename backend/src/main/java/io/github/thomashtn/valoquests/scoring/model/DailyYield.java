package io.github.thomashtn.valoquests.scoring.model;

/**
 * Where a player stands on one day's diminishing-returns ladder, before their next match.
 *
 * <p>Elsewhere the ladder prices existing matches; telling what the next one will be worth is what can
 * actually discourage a marathon session.
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
