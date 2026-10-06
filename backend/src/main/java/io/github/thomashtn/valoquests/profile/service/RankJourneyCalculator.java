package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.profile.dto.PlayerProgressionResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Computes where each season left a player on the competitive ladder.
 *
 * <p>Pure: takes the matches, touches no repository.</p>
 */
final class RankJourneyCalculator {

    /**
     * Not instantiable: static helpers only.
     */
    private RankJourneyCalculator() {
    }

    /**
     * Summarizes every season the player held a rank in.
     *
     * @param competitive     competitive matches of the selected seasons, oldest first
     * @param currentSeasonId season in progress, or {@code null} when none is known
     * @return one entry per season with at least one ranked match, oldest first
     */
    static List<PlayerProgressionResponse.SeasonRank> journey(
        List<PlayerMatch> competitive,
        Long currentSeasonId
    ) {
        return SeasonGrouping.bySeason(competitive).values().stream()
            .map(matches -> toSeasonRank(matches, currentSeasonId))
            .flatMap(Optional::stream)
            .toList();
    }

    /**
     * Summarizes one season's ranks.
     *
     * @param matches         one season's competitive matches, oldest first
     * @param currentSeasonId season in progress, or {@code null} when none is known
     * @return the season's ranks, or empty when the season held only placements
     */
    private static Optional<PlayerProgressionResponse.SeasonRank> toSeasonRank(
        List<PlayerMatch> matches,
        Long currentSeasonId
    ) {
        List<CompetitiveTier> ranked = matches.stream()
            .map(PlayerMatch::getCompetitiveTier)
            .filter(tier -> tier != null && tier != CompetitiveTier.UNRANKED)
            .toList();
        if (ranked.isEmpty()) {
            return Optional.empty();
        }

        Season season = matches.getFirst().getMatch().getSeason();
        return Optional.of(new PlayerProgressionResponse.SeasonRank(
            season.getId(),
            season.getName(),
            season.getId().equals(currentSeasonId),
            ranked.getLast(),
            Collections.max(ranked),
            Collections.min(ranked),
            matches.size(),
            matches.stream().filter(match -> match.getResult() == MatchResult.WIN).count(),
            ranked
        ));
    }
}
