package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.profile.dto.PlayerProgressionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Computes how steady a player's combat score was over the selected seasons.
 *
 * <p>Pure: takes the matches, touches no repository.</p>
 */
final class ConsistencyCalculator {

    /**
     * Scored matches a set needs before its spread means anything, as smaller quartiles move with each game.
     */
    private static final int MINIMUM_SAMPLE = 8;

    /**
     * Not instantiable: static helpers only.
     */
    private ConsistencyCalculator() {
    }

    /**
     * Summarizes the combat-score spread of the selection.
     *
     * <p>A single-season selection is also compared with the previous season, read from the career.
     *
     * @param selected competitive matches of the selected seasons, oldest first
     * @param career   competitive matches of the player's whole career, oldest first
     * @return the spread, or {@code null} when the selection holds too few scored matches
     */
    static PlayerProgressionResponse.Consistency consistency(
        List<PlayerMatch> selected,
        List<PlayerMatch> career
    ) {
        List<PlayerMatch> scored = scored(selected);
        if (scored.size() < MINIMUM_SAMPLE) {
            return null;
        }

        Map<Long, List<PlayerMatch>> seasons = SeasonGrouping.bySeason(scored);
        List<BigDecimal> sorted = sortedScores(scored);
        BigDecimal floor = quantile(sorted, 1);
        BigDecimal ceiling = quantile(sorted, 3);
        List<PlayerMatch> previous = seasons.size() == 1
            ? previousSeason(seasons.keySet().iterator().next(), career)
            : List.of();

        return new PlayerProgressionResponse.Consistency(
            floor,
            quantile(sorted, 2),
            ceiling,
            ceiling.subtract(floor),
            seasons.size(),
            previous.isEmpty() ? null : previous.getFirst().getMatch().getSeason().getName(),
            previous.isEmpty() ? null : spread(sortedScores(previous)),
            scored.stream().map(ConsistencyCalculator::toMatch).toList()
        );
    }

    /**
     * Finds the last season before the given one that holds enough scored matches.
     *
     * @param seasonId the season to look before
     * @param career   competitive matches of the whole career, oldest first
     * @return that season's scored matches, or an empty list when there is none
     */
    private static List<PlayerMatch> previousSeason(long seasonId, List<PlayerMatch> career) {
        List<PlayerMatch> previous = List.of();
        for (Map.Entry<Long, List<PlayerMatch>> season : SeasonGrouping.bySeason(scored(career)).entrySet()) {
            if (season.getKey() == seasonId) {
                return previous;
            }
            if (season.getValue().size() >= MINIMUM_SAMPLE) {
                previous = season.getValue();
            }
        }
        return List.of();
    }

    /**
     * Keeps the matches that carry a combat score.
     *
     * @param matches matches in any state
     * @return the scored ones, in the same order
     */
    private static List<PlayerMatch> scored(List<PlayerMatch> matches) {
        return matches.stream().filter(match -> match.getAcs() != null).toList();
    }

    /**
     * Reads the combat scores in ascending order.
     *
     * @param matches scored matches
     * @return their combat scores, sorted
     */
    private static List<BigDecimal> sortedScores(List<PlayerMatch> matches) {
        return matches.stream().map(PlayerMatch::getAcs).sorted().toList();
    }

    /**
     * Measures the gap between the third and the first quartile.
     *
     * @param sorted values in ascending order, never empty
     * @return the spread
     */
    private static BigDecimal spread(List<BigDecimal> sorted) {
        return quantile(sorted, 3).subtract(quantile(sorted, 1));
    }

    /**
     * Reads a quartile by linear interpolation between the two closest ranks.
     *
     * @param sorted   values in ascending order, never empty
     * @param quartile which quartile to read, from 1 to 3
     * @return the quartile, rounded half up to two decimals
     */
    private static BigDecimal quantile(List<BigDecimal> sorted, int quartile) {
        int scaledPosition = (sorted.size() - 1) * quartile;
        int lower = scaledPosition / 4;
        BigDecimal fraction = BigDecimal.valueOf(scaledPosition % 4)
            .divide(BigDecimal.valueOf(4), 2, RoundingMode.UNNECESSARY);
        BigDecimal base = sorted.get(lower);
        if (fraction.signum() == 0) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        return base.add(sorted.get(lower + 1).subtract(base).multiply(fraction))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Turns one match into a point on the consistency chart.
     *
     * @param match the match to plot
     * @return that match's point
     */
    private static PlayerProgressionResponse.ConsistencyMatch toMatch(PlayerMatch match) {
        return new PlayerProgressionResponse.ConsistencyMatch(
            match.getMatch().getStartedAt(),
            match.getAcs(),
            match.getResult(),
            match.allyScore(),
            match.enemyScore(),
            match.getMatch().getMapName(),
            match.getAgentName()
        );
    }
}
