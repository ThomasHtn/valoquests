package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Splits a player's matches into one group per season.
 */
final class SeasonGrouping {

    /**
     * Not instantiable: static helpers only.
     */
    private SeasonGrouping() {
    }

    /**
     * Groups matches by season, keeping the seasons in the order their first match appears.
     *
     * @param matches matches to group, oldest first
     * @return each season's matches, oldest season first and oldest match first within it
     */
    static Map<Long, List<PlayerMatch>> bySeason(List<PlayerMatch> matches) {
        return matches.stream()
            .collect(Collectors.groupingBy(
                match -> match.getMatch().getSeason().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
}
