package io.github.thomashtn.valoquests.match.repository;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import java.time.Instant;

/**
 * Bundles {@link PlayerMatchRepository#findHistory} filter criteria into one parameter, keeping the
 * method under the project's parameter-count limit. {@code seasonId}, {@code map}, {@code agent},
 * {@code result} and {@code gameMode} are optional and ignored when {@code null}.
 *
 * <p>{@code periodStart}/{@code periodEnd} form a half-open range, inclusive beginning and exclusive
 * end, but - unlike the other fields - must never be {@code null}: callers with no week filter pass
 * {@link #UNBOUNDED_PERIOD_START}/{@link #UNBOUNDED_PERIOD_END} instead. PostgreSQL determines a bind
 * parameter's data type from how it is used in the query text alone, at statement-prepare time,
 * before any value is bound; a placeholder that appears only inside a {@code :param IS NULL OR ...}
 * check - with no other, typed usage - leaves it unable to do so for a temporal parameter ("could not
 * determine data type of parameter"), regardless of whether the bound value later turns out to be
 * null or not. Always supplying a concrete bound removes the {@code IS NULL} branch from the query
 * entirely, which sidesteps the issue without needing an explicit cast - the fix used for
 * {@code map}/{@code agent} instead, which does not carry over here: casting a null value bound with
 * no type hint fails the same way ({@code cannot cast type bytea to timestamp with time zone}).
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
     * Stand-in {@code periodStart} for callers with no week filter, well before any Valorant match
     * could have been played.
     */
    public static final Instant UNBOUNDED_PERIOD_START = Instant.parse("2000-01-01T00:00:00Z");

    /**
     * Stand-in {@code periodEnd} for callers with no week filter, comfortably beyond any match this
     * application will ever record.
     */
    public static final Instant UNBOUNDED_PERIOD_END = Instant.parse("2100-01-01T00:00:00Z");
}
