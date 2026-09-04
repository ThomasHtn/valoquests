package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Component;

/**
 * Awards the four weekly honours from a week's ranking rows.
 *
 * <p>A tie awards nothing. Two operators who both did the most are not both the most, and a title
 * that can be shared stops meaning anything the first time it is.
 *
 * <p>Reads the ranking rather than the campaign: the rows already carry what each operator's week
 * was worth, and they exist between two campaigns too, so a squad keeps its honours while it waits
 * for the next one. Only ranked rows compete: an inactive player's counts are for their own eyes.
 */
@Component
public class WeeklyTitleResolver {

    /**
     * Awards one week's titles.
     *
     * @param scores the week's ranking rows
     * @return the holder of each title, titles nobody won outright omitted
     */
    public Map<WeeklyTitle, Long> resolve(List<WeeklyPlayerScore> scores) {
        List<WeeklyPlayerScore> ranked = scores.stream()
            .filter(score -> score.getPosition() != null)
            .toList();

        Map<WeeklyTitle, Long> titles = new EnumMap<>(WeeklyTitle.class);
        award(titles, ranked, WeeklyTitle.MECHANIC, WeeklyPlayerScore::getComponents);
        award(titles, ranked, WeeklyTitle.QUARTERMASTER, WeeklyPlayerScore::getFood);
        award(titles, ranked, WeeklyTitle.REGULAR, WeeklyPlayerScore::getStreakDays);
        award(titles, ranked, WeeklyTitle.SCOUT, WeeklyPlayerScore::completedAllChallenges);

        return titles;
    }

    /**
     * Awards one title to the single operator holding the highest figure, if there is one.
     *
     * @param titles titles awarded so far
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
        int best = ranked.stream().mapToInt(figure).max().orElse(0);

        if (best <= 0) {
            return;
        }

        List<WeeklyPlayerScore> leaders = ranked.stream()
            .filter(score -> figure.applyAsInt(score) == best)
            .toList();

        if (leaders.size() == 1) {
            titles.put(title, leaders.getFirst().getPlayer().getId());
        }
    }
}
