package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.dto.AgentStatisticsResponse;
import io.github.thomashtn.valoquests.player.dto.MapStatisticsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerDetailsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Aggregates a set of player matches into the performance indicators every profile screen reads.
 *
 * <p>Shared by {@link DefaultPlayerQueryService} and
 * {@link DefaultPlayerProgressionQueryService}: both reduce arbitrary groupings of matches - a
 * whole season, one agent, one map, one weekday - with exactly these formulas, and a second
 * implementation would let the two screens disagree on what a win rate is.
 *
 * @param matchesPlayed      number of matches in the group
 * @param wins               matches whose result is {@link MatchResult#WIN}
 * @param losses             matches whose result is {@link MatchResult#LOSS}
 * @param kills              total kills
 * @param deaths             total deaths
 * @param assists            total assists
 * @param mvps               matches finished with the best score of the game
 * @param kda                ratio of kills and assists to deaths
 * @param winRate            share of matches won, as a percentage
 * @param adr                average damage per round
 * @param acs                average combat score
 * @param headshotPercentage share of hits that landed on the head, as a percentage
 */
record MatchStatistics(
    long matchesPlayed,
    long wins,
    long losses,
    long kills,
    long deaths,
    long assists,
    long mvps,
    BigDecimal kda,
    BigDecimal winRate,
    BigDecimal adr,
    BigDecimal acs,
    BigDecimal headshotPercentage
) {

    /**
     * Reduces a group of matches into its aggregate indicators.
     *
     * <p>An empty group yields zeroes rather than nulls, so a caller never has to special-case a
     * player who has not played the agent, map or period being summarized.
     *
     * @param matches matches to aggregate; never {@code null}
     * @return the group's aggregate indicators
     */
    static MatchStatistics from(List<PlayerMatch> matches) {
        long wins = matches.stream().filter(match -> match.getResult() == MatchResult.WIN).count();
        long losses = matches.stream().filter(match -> match.getResult() == MatchResult.LOSS).count();
        long kills = matches.stream().mapToLong(PlayerMatch::getKills).sum();
        long deaths = matches.stream().mapToLong(PlayerMatch::getDeaths).sum();
        long assists = matches.stream().mapToLong(PlayerMatch::getAssists).sum();
        long mvps = matches.stream().filter(PlayerMatch::isMvp).count();
        long headshots = matches.stream().mapToLong(PlayerMatch::getHeadshots).sum();
        long shots = matches.stream().mapToLong(match ->
            match.getHeadshots() + match.getBodyshots() + match.getLegshots()
        ).sum();
        BigDecimal kda = divide(kills + assists, Math.max(1, deaths));
        BigDecimal winRate = percentage(wins, matches.size());
        BigDecimal adr = average(matches.stream().map(PlayerMatch::getAdr).toList());
        BigDecimal acs = average(matches.stream().map(PlayerMatch::getAcs).toList());
        BigDecimal headshotPercentage = percentage(headshots, shots);
        return new MatchStatistics(
            matches.size(), wins, losses, kills, deaths, assists, mvps,
            kda, winRate, adr, acs, headshotPercentage
        );
    }

    /**
     * Averages the non-null values of a list, ignoring the missing ones.
     *
     * <p>{@code acs} and {@code adr} are nullable on {@link PlayerMatch}: some game modes report no
     * round count, so counting those matches in the denominator would drag the average toward zero
     * for a player who simply has no figure to report.
     *
     * @param values values to average, possibly containing nulls
     * @return the average of the non-null values, or zero when none remain
     */
    static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(value -> value != null).toList();
        if (nonNull.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(nonNull.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Divides two counts into a two-decimal ratio.
     *
     * @param numerator   dividend
     * @param denominator divisor; must not be zero
     * @return the ratio, rounded half up to two decimals
     */
    static BigDecimal divide(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /**
     * Expresses a count as a percentage of a total.
     *
     * @param numerator   counted occurrences
     * @param denominator total occurrences; zero yields zero rather than failing
     * @return the percentage, rounded half up to two decimals
     */
    static BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /**
     * Summarizes every match a player played on one agent.
     *
     * @param agentName agent the group was played on
     * @param matches   that agent's matches
     * @return the agent's aggregated statistics
     */
    static AgentStatisticsResponse toAgentStatistics(String agentName, List<PlayerMatch> matches) {
        MatchStatistics statistics = from(matches);
        String agentId = firstIdentifier(matches, PlayerMatch::getAgentId);
        return new AgentStatisticsResponse(
            agentId,
            agentName,
            statistics.matchesPlayed(),
            statistics.wins(),
            statistics.losses(),
            statistics.winRate(),
            statistics.kda(),
            statistics.adr(),
            statistics.acs()
        );
    }

    /**
     * Summarizes every match a player played on one map.
     *
     * @param mapName map the group was played on
     * @param matches that map's matches
     * @return the map's aggregated statistics
     */
    static MapStatisticsResponse toMapStatistics(String mapName, List<PlayerMatch> matches) {
        MatchStatistics statistics = from(matches);
        String mapId = firstIdentifier(matches, match -> match.getMatch().getMapId());
        return new MapStatisticsResponse(
            mapId,
            mapName,
            statistics.matchesPlayed(),
            statistics.wins(),
            statistics.losses(),
            statistics.winRate(),
            statistics.kda(),
            statistics.adr(),
            statistics.acs()
        );
    }

    /**
     * Picks the first stable identifier present in a group of matches.
     *
     * <p>Henrik does not always return one, and it is the same value for every match in the group
     * when it does, so the first non-blank occurrence stands for the whole group.
     *
     * @param matches   matches to read from
     * @param extractor reads the candidate identifier off one match
     * @return the first non-blank identifier, or {@code null} when the group carries none
     */
    private static String firstIdentifier(List<PlayerMatch> matches, Function<PlayerMatch, String> extractor) {
        return matches.stream()
            .map(extractor)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    /**
     * Projects these indicators onto the profile-statistics payload.
     *
     * @return the aggregate statistics as exposed by the player-details endpoint
     */
    PlayerDetailsResponse.PlayerStatistics toResponse() {
        return new PlayerDetailsResponse.PlayerStatistics(
            kda, winRate, adr, acs, headshotPercentage,
            kills, deaths, assists, matchesPlayed, wins, losses, mvps
        );
    }
}
