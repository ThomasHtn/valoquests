package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.profile.dto.AgentStatisticsResponse;
import io.github.thomashtn.valoquests.profile.dto.MapStatisticsResponse;
import io.github.thomashtn.valoquests.profile.dto.PlayerDetailsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        long shots = matches.stream().mapToLong(PlayerMatch::totalShots).sum();
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
    private static BigDecimal average(List<BigDecimal> values) {
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
     * Summarizes the matches agent by agent, most played first and then by name.
     *
     * @param matches matches to aggregate; those without an agent are left out
     * @return one entry per agent
     */
    static List<AgentStatisticsResponse> perAgent(List<PlayerMatch> matches) {
        return groupBy(matches, PlayerMatch::getAgentName).entrySet().stream()
            .map(entry -> toAgentStatistics(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingLong(AgentStatisticsResponse::matchesPlayed).reversed()
                .thenComparing(AgentStatisticsResponse::agentName))
            .toList();
    }

    /**
     * Summarizes the matches map by map, most played first and then by name.
     *
     * @param matches matches to aggregate; those without a map are left out
     * @return one entry per map
     */
    static List<MapStatisticsResponse> perMap(List<PlayerMatch> matches) {
        return groupBy(matches, match -> match.getMatch().getMapName()).entrySet().stream()
            .map(entry -> toMapStatistics(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingLong(MapStatisticsResponse::matchesPlayed).reversed()
                .thenComparing(MapStatisticsResponse::mapName))
            .toList();
    }

    /**
     * Groups matches by a key, dropping the ones the key cannot be read from.
     */
    private static Map<String, List<PlayerMatch>> groupBy(
        List<PlayerMatch> matches,
        Function<PlayerMatch, String> classifier
    ) {
        return matches.stream()
            .filter(match -> classifier.apply(match) != null)
            .collect(Collectors.groupingBy(classifier));
    }

    /**
     * Summarizes every match a player played on one agent.
     */
    private static AgentStatisticsResponse toAgentStatistics(String agentName, List<PlayerMatch> matches) {
        MatchStatistics statistics = from(matches);
        return new AgentStatisticsResponse(
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
     */
    private static MapStatisticsResponse toMapStatistics(String mapName, List<PlayerMatch> matches) {
        MatchStatistics statistics = from(matches);
        return new MapStatisticsResponse(
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
