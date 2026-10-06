package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.profile.dto.PlayerProgressionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Computes a player's match-by-match evolution per season and where their hits landed.
 *
 * <p>Pure: takes the matches, touches no repository.</p>
 */
final class EvolutionCalculator {

    /**
     * Not instantiable: static helpers only.
     */
    private EvolutionCalculator() {
    }

    /**
     * Builds one match-by-match series per season, oldest season first.
     *
     * @param matches         competitive matches in scope, oldest first
     * @param currentSeasonId season in progress, or {@code null} when none is known
     * @return one entry per season the player actually played in scope
     */
    static List<PlayerProgressionResponse.SeasonEvolution> evolution(
        List<PlayerMatch> matches,
        Long currentSeasonId
    ) {
        return SeasonGrouping.bySeason(matches).values().stream()
            .map(seasonMatches -> toSeasonEvolution(seasonMatches, currentSeasonId))
            .toList();
    }

    /**
     * Sums where the player's hits landed over the whole filtered set.
     *
     * @param matches competitive matches in scope
     * @return the aim breakdown
     */
    static PlayerProgressionResponse.AimBreakdown aim(List<PlayerMatch> matches) {
        long head = matches.stream().mapToLong(PlayerMatch::getHeadshots).sum();
        long body = matches.stream().mapToLong(PlayerMatch::getBodyshots).sum();
        long leg = matches.stream().mapToLong(PlayerMatch::getLegshots).sum();
        long total = head + body + leg;
        return new PlayerProgressionResponse.AimBreakdown(
            MatchStatistics.percentage(head, total),
            MatchStatistics.percentage(body, total),
            MatchStatistics.percentage(leg, total),
            total
        );
    }

    /**
     * Turns one season's matches into its plotted series and its legend averages.
     *
     * <p>Averages are the season's aggregate indicators, not the mean of the points, to match the summary
     * tiles.
     *
     * @param matches         one season's competitive matches, oldest first
     * @param currentSeasonId season in progress, or {@code null} when none is known
     * @return the season's evolution entry
     */
    private static PlayerProgressionResponse.SeasonEvolution toSeasonEvolution(
        List<PlayerMatch> matches,
        Long currentSeasonId
    ) {
        Season season = matches.get(0).getMatch().getSeason();
        MatchStatistics statistics = MatchStatistics.from(matches);
        return new PlayerProgressionResponse.SeasonEvolution(
            season.getId(),
            season.getName(),
            season.getId().equals(currentSeasonId),
            matches.stream().map(EvolutionCalculator::toMatchPoint).toList(),
            new PlayerProgressionResponse.Averages(
                statistics.headshotPercentage(),
                statistics.kda(),
                statistics.acs(),
                statistics.adr()
            )
        );
    }

    /**
     * Turns one match into a point on the evolution charts.
     *
     * @param match the match to plot
     * @return that match's point
     */
    private static PlayerProgressionResponse.MatchPoint toMatchPoint(PlayerMatch match) {
        return new PlayerProgressionResponse.MatchPoint(
            match.getMatch().getStartedAt(),
            Objects.requireNonNullElse(match.headshotPercentage(), BigDecimal.ZERO),
            match.kda(),
            match.getAcs(),
            match.getAdr()
        );
    }
}
