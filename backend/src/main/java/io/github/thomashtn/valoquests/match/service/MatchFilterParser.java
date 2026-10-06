package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

/**
 * Turns the raw match filters of a request into repository criteria, rejecting unknown values.
 */
public final class MatchFilterParser {

    /**
     * Not instantiable: static helpers only.
     */
    private MatchFilterParser() {
    }

    /**
     * Builds the criteria of one filtered match history.
     *
     * @param filter       raw filters, every one optional
     * @param weekCalendar calendar resolving the week's instant bounds
     * @return the criteria, unbounded in time without a week filter
     */
    public static PlayerMatchHistoryCriteria toCriteria(MatchHistoryFilter filter, WeekCalendar weekCalendar) {
        LocalDate weekStart = filter.weekStart();

        return new PlayerMatchHistoryCriteria(
            filter.seasonId(),
            text(filter.map()),
            text(filter.agent()),
            result(filter.result()),
            gameMode(filter.gameMode()),
            weekStart == null
                ? PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START : weekCalendar.weekStartInstant(weekStart),
            weekStart == null
                ? PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END : weekCalendar.weekEndInstant(weekStart)
        );
    }

    /**
     * Reads the result filter, {@code null} when absent.
     */
    private static MatchResult result(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MatchResult.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("result must be WIN, LOSS or DRAW", exception);
        }
    }

    /**
     * Reads the game mode filter, {@code null} when absent.
     */
    private static GameMode gameMode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GameMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                "gameMode must be one of " + Arrays.toString(GameMode.values()),
                exception
            );
        }
    }

    /**
     * Trims a text filter, {@code null} when blank.
     */
    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
