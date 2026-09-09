package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.dto.PlayerProgressionResponse;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Computes a player's personal bests out of their matches.
 *
 * <p>Pure: takes the matches and the calendar zone, touches no repository.</p>
 */
final class PlayerRecordsCalculator {

    /**
     * Rounds a match must hold before its headshot rate can count as a record.
     */
    private static final int MINIMUM_HEADSHOT_ROUNDS = 10;

    private PlayerRecordsCalculator() {
    }

    /**
     * Collects the player's personal bests.
     *
     * @param competitive competitive matches in scope, oldest first
     * @param everyMode   every match in scope regardless of game mode, used by the day streak
     * @param zone        calendar zone the day streak is counted in
     * @return the personal records
     */
    static PlayerProgressionResponse.PersonalRecords records(
        List<PlayerMatch> competitive,
        List<PlayerMatch> everyMode,
        ZoneId zone
    ) {
        List<PlayerMatch> longEnough = competitive.stream()
            .filter(match -> match.getRoundsPlayed() >= MINIMUM_HEADSHOT_ROUNDS)
            .toList();

        return new PlayerProgressionResponse.PersonalRecords(
            best(competitive, match -> BigDecimal.valueOf(match.getKills())),
            best(competitive, PlayerMatch::getAcs),
            best(competitive, match -> BigDecimal.valueOf(match.getDamageDealt())),
            best(competitive, PlayerRecordsCalculator::matchKda),
            best(longEnough, PlayerRecordsCalculator::headshotPercentage),
            longestWinStreak(competitive),
            longestActiveDayStreak(everyMode, zone),
            competitive.stream().filter(PlayerMatch::isMvp).count(),
            peakTier(competitive)
        );
    }

    /**
     * Reads one match's headshot rate.
     *
     * @param match the match to measure
     * @return the share of that match's hits that landed on the head
     */
    static BigDecimal headshotPercentage(PlayerMatch match) {
        long shots = (long) match.getHeadshots() + match.getBodyshots() + match.getLegshots();
        return MatchStatistics.percentage(match.getHeadshots(), shots);
    }

    /**
     * Reads one match's ratio of kills and assists to deaths.
     *
     * @param match the match to measure
     * @return that match's KDA ratio
     */
    static BigDecimal matchKda(PlayerMatch match) {
        return MatchStatistics.divide(
            (long) match.getKills() + match.getAssists(),
            Math.max(1, match.getDeaths())
        );
    }

    /**
     * Finds the match holding the highest value of one metric.
     *
     * <p>Zero and negative values never qualify: a personal best of nothing is not a record, and
     * reporting one would fill the section with empty boasts on a freshly synchronized player.
     *
     * @param matches   matches to search
     * @param extractor reads the metric off one match, possibly returning {@code null}
     * @return the record, or {@code null} when no match qualifies
     */
    private static PlayerProgressionResponse.RecordEntry best(
        List<PlayerMatch> matches,
        Function<PlayerMatch, BigDecimal> extractor
    ) {
        PlayerMatch holder = null;
        BigDecimal record = null;
        for (PlayerMatch match : matches) {
            BigDecimal candidate = extractor.apply(match);
            if (candidate == null || candidate.signum() <= 0) {
                continue;
            }
            if (record == null || candidate.compareTo(record) > 0) {
                holder = match;
                record = candidate;
            }
        }
        if (holder == null) {
            return null;
        }
        return new PlayerProgressionResponse.RecordEntry(
            record,
            holder.getMatch().getStartedAt(),
            holder.getMatch().getMapName(),
            holder.getAgentName()
        );
    }

    /**
     * Measures the longest run of consecutive wins.
     *
     * <p>Remakes are skipped rather than counted as a loss: a game that never really happened
     * should not break a streak the player did earn.
     *
     * @param matches competitive matches in scope, oldest first
     * @return the longest run of wins, or zero when the player never won
     */
    private static int longestWinStreak(List<PlayerMatch> matches) {
        int longest = 0;
        int current = 0;
        for (PlayerMatch match : matches) {
            if (match.getResult() == MatchResult.REMAKE) {
                continue;
            }
            current = match.getResult() == MatchResult.WIN ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    /**
     * Measures the longest run of consecutive calendar days holding at least one match.
     *
     * <p>Counted over every game mode, deliberately: this records showing up, not competing, so a
     * night of deathmatch keeps the run alive.
     *
     * @param matches every match in scope, in any order
     * @param zone    calendar zone the days are counted in
     * @return the longest run of active days, or zero when the player played nothing
     */
    private static int longestActiveDayStreak(List<PlayerMatch> matches, ZoneId zone) {
        List<LocalDate> days = matches.stream()
            .map(match -> match.getMatch().getStartedAt().atZone(zone).toLocalDate())
            .distinct()
            .sorted()
            .toList();

        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            boolean consecutive = previous != null && day.equals(previous.plusDays(1));
            current = consecutive ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = day;
        }
        return longest;
    }

    /**
     * Finds the highest competitive rank the player held during the filtered matches.
     *
     * @param matches competitive matches in scope
     * @return the peak tier, or {@code null} when no match carries a rank
     */
    private static CompetitiveTier peakTier(List<PlayerMatch> matches) {
        return matches.stream()
            .map(PlayerMatch::getCompetitiveTier)
            .filter(tier -> tier != null && tier != CompetitiveTier.UNRANKED)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }
}
