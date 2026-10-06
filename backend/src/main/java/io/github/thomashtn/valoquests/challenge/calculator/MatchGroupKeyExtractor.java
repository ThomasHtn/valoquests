package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;

/**
 * Reads the key a grouped challenge files one match under.
 */
final class MatchGroupKeyExtractor {

    /**
     * Calendar placing a match on the calendar day it counts towards.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the group key extractor.
     *
     * @param weekCalendar calendar resolving the day a match belongs to
     */
    MatchGroupKeyExtractor(WeekCalendar weekCalendar) {
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the key one match is grouped under.
     *
     * @param playerMatch persisted player-match data
     * @param groupBy     requested grouping dimension
     * @return grouping key, or {@code null} when unavailable
     */
    Object keyOf(PlayerMatch playerMatch, ChallengeGroupBy groupBy) {
        return switch (groupBy) {
            case AGENT -> agentKeyOf(playerMatch);
            case GAME_MODE -> playerMatch.getMatch().getGameMode();
            case PLAY_DAY -> weekCalendar.dayOf(playerMatch.getMatch().getStartedAt());
        };
    }

    /**
     * Returns the most stable available agent identifier: its id, else its name.
     */
    private static String agentKeyOf(PlayerMatch playerMatch) {
        if (playerMatch.getAgentId() != null && !playerMatch.getAgentId().isBlank()) {
            return playerMatch.getAgentId();
        }

        if (playerMatch.getAgentName() != null && !playerMatch.getAgentName().isBlank()) {
            return playerMatch.getAgentName();
        }

        return null;
    }
}
