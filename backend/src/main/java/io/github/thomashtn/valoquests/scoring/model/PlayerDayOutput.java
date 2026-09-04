package io.github.thomashtn.valoquests.scoring.model;

/**
 * What one player's matches produced over one calendar day, after both multipliers.
 *
 * @param damage             damage dealt to the guardian, food and components summed
 * @param food               food produced
 * @param components         components produced
 * @param matchCount         valued matches played that day
 * @param reducedMatchCount  valued matches priced below full value by the daily diminishing returns
 * @param streakDays         consecutive played days ending on this day, this day included
 * @param streakBonusPercent bonus every match of the day earned from that streak
 */
public record PlayerDayOutput(
    int damage,
    int food,
    int components,
    int matchCount,
    int reducedMatchCount,
    int streakDays,
    int streakBonusPercent
) {

    /**
     * What a day without any valued match produced: nothing.
     */
    public static final PlayerDayOutput NONE = new PlayerDayOutput(0, 0, 0, 0, 0, 0, 0);

    /**
     * Folds one valued match into this day's totals.
     *
     * @param match valued match played on this day
     * @return a new total, streak figures taken from the match
     */
    public PlayerDayOutput plus(ValuedMatch match) {
        boolean reduced = match.coefficientPercent() < ValuedMatch.FULL_COEFFICIENT_PERCENT;

        return new PlayerDayOutput(
            damage + match.damage(),
            food + match.food(),
            components + match.components(),
            matchCount + 1,
            reducedMatchCount + (reduced ? 1 : 0),
            match.streakDays(),
            match.streakBonusPercent()
        );
    }
}
