package io.github.thomashtn.valoquests.scoring.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One valued match, priced once with both multipliers and split into its two resources.
 *
 * <p>{@code damage} is the whole of what the match produced; {@code food} and {@code components}
 * always add up to it, so a consumer may read either the total or the split without reconciling.
 *
 * @param playerMatchId      internal player-match identifier
 * @param playerId           internal identifier of the player who played it
 * @param startedAt          instant the match started, the chronology the finishing blow is decided on
 * @param day                calendar day of the project's zone the match belongs to
 * @param baseDamage         value of the match before any multiplier
 * @param coefficientPercent share kept after the day's diminishing returns
 * @param streakDays         consecutive played days ending on the match's day, that day included
 * @param streakBonusPercent bonus earned from that streak
 * @param damage             value after both multipliers, rounded once
 * @param food               food share of that value
 * @param components         components share of that value
 */
public record ValuedMatch(
    Long playerMatchId,
    Long playerId,
    Instant startedAt,
    LocalDate day,
    int baseDamage,
    int coefficientPercent,
    int streakDays,
    int streakBonusPercent,
    int damage,
    int food,
    int components
) {

    /**
     * Coefficient of a match the daily diminishing returns left untouched.
     */
    public static final int FULL_COEFFICIENT_PERCENT = 100;
}
