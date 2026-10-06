package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Component;

/**
 * Awards the four weekly honours from a week's ranking rows.
 *
 * <p>Awarded in declaration order, one per player, champions excluded; a tie awards nothing. Only ranked
 * rows compete, and reading them rather than the campaign keeps honours between campaigns.
 */
@Component
public class WeeklyTitleResolver {

    /**
     * Awards one week's titles, leaving out both its own champion and the one reigning over it.
     *
     * @param scores             the week's ranking rows
     * @param weekChampionId     the week's own champion, or {@code null} while the week is in progress
     * @param reigningChampionId the previous week's champion, who wore the crown that week, or {@code null}
     * @return the holder of each title, titles nobody won outright omitted
     */
    public Map<WeeklyTitle, Long> resolve(
        List<WeeklyPlayerScore> scores,
        Long weekChampionId,
        Long reigningChampionId
    ) {
        List<WeeklyPlayerScore> ranked = scores.stream()
            .filter(score -> score.getPosition() != null)
            .filter(score -> !Objects.equals(score.getPlayer().getId(), weekChampionId))
            .filter(score -> !Objects.equals(score.getPlayer().getId(), reigningChampionId))
            .toList();

        Map<WeeklyTitle, Long> titles = new EnumMap<>(WeeklyTitle.class);
        award(titles, ranked, WeeklyTitle.REGULAR, WeeklyPlayerScore::getPlayedDays);
        award(titles, ranked, WeeklyTitle.SCOUT, WeeklyPlayerScore::totalCompletedChallenges);
        award(titles, ranked, WeeklyTitle.QUARTERMASTER, WeeklyPlayerScore::getFood);
        award(titles, ranked, WeeklyTitle.MECHANIC, WeeklyPlayerScore::getComponents);

        return titles;
    }

    /**
     * Awards one title to the single untitled player holding the highest figure, if there is one.
     *
     * @param titles titles awarded so far, whose holders no longer compete
     * @param ranked the week's ranked rows
     * @param title  title being awarded
     * @param figure figure the title is awarded on
     */
    private void award(
        Map<WeeklyTitle, Long> titles,
        List<WeeklyPlayerScore> ranked,
        WeeklyTitle title,
        ToIntFunction<WeeklyPlayerScore> figure
    ) {
        List<WeeklyPlayerScore> untitled = ranked.stream()
            .filter(score -> !titles.containsValue(score.getPlayer().getId()))
            .toList();
        int best = untitled.stream().mapToInt(figure).max().orElse(0);

        if (best <= 0) {
            return;
        }

        List<WeeklyPlayerScore> leaders = untitled.stream()
            .filter(score -> figure.applyAsInt(score) == best)
            .toList();

        if (leaders.size() == 1) {
            titles.put(title, leaders.getFirst().getPlayer().getId());
        }
    }
}
