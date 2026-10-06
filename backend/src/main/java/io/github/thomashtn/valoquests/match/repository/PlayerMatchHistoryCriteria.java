package io.github.thomashtn.valoquests.match.repository;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import java.time.Instant;

/**
 * Bundles {@link PlayerMatchRepository#findHistory} filter criteria into one parameter.
 *
 * <p>The period bounds must never be {@code null}, since PostgreSQL cannot type a null temporal
 * parameter: pass {@link #UNBOUNDED_PERIOD_START}/{@link #UNBOUNDED_PERIOD_END} instead.
 *
 * @param seasonId    internal season identifier, or {@code null} for every season
 * @param map         map name, matched case-insensitively, or {@code null} for every map
 * @param agent       agent name, matched case-insensitively, or {@code null} for every agent
 * @param result      match outcome, or {@code null} for every outcome
 * @param gameMode    game mode, or {@code null} for every mode
 * @param periodStart inclusive beginning of the week range; never {@code null}
 * @param periodEnd   exclusive end of the week range; never {@code null}
 */
public record PlayerMatchHistoryCriteria(
    Long seasonId,
    String map,
    String agent,
    MatchResult result,
    GameMode gameMode,
    Instant periodStart,
    Instant periodEnd
) {

    /**
     * Stand-in {@code periodStart} for callers with no week filter, before any Valorant match.
     */
    public static final Instant UNBOUNDED_PERIOD_START = Instant.parse("2000-01-01T00:00:00Z");

    /**
     * Stand-in {@code periodEnd} for callers with no week filter, beyond any recorded match.
     */
    public static final Instant UNBOUNDED_PERIOD_END = Instant.parse("2100-01-01T00:00:00Z");
}
