package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the four weekly honours, and that a tie awards none of them.
 */
class WeeklyTitleResolverTest {

    /**
     * First ranked player.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Second ranked player.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    /**
     * Player listed without a slot.
     */
    private static final Player CHARLIE = RankingFixtures.player(3, "Charlie", PlayerStatus.INACTIVE);

    private final WeeklyTitleResolver resolver = new WeeklyTitleResolver();

    @Test
    @DisplayName("Awards each title to the single best figure of the week")
    void shouldAwardEachTitleToTheBest() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 900, 100, 3, 1, 0);
        WeeklyPlayerScore bravo = row(BRAVO, 2, 200, 800, 5, 2, 2);

        Map<WeeklyTitle, Long> titles = resolver.resolve(List.of(alpha, bravo));

        assertThat(titles).containsExactlyInAnyOrderEntriesOf(Map.of(
            WeeklyTitle.MECHANIC, ALPHA.getId(),
            WeeklyTitle.QUARTERMASTER, BRAVO.getId(),
            WeeklyTitle.REGULAR, BRAVO.getId(),
            WeeklyTitle.SCOUT, BRAVO.getId()
        ));
    }

    @Test
    @DisplayName("Awards nothing on a tie, and nothing on a week where nobody produced")
    void shouldAwardNothingOnATieOrAnEmptyWeek() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 500, 0, 0, 0, 0);
        WeeklyPlayerScore bravo = row(BRAVO, 2, 500, 0, 0, 0, 0);

        assertThat(resolver.resolve(List.of(alpha, bravo))).isEmpty();
        assertThat(resolver.resolve(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Never awards a title to a row without a slot")
    void shouldIgnoreUnrankedRows() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 0, 0, 0, 1, 0);
        WeeklyPlayerScore charlie = row(CHARLIE, null, 0, 0, 0, 5, 3);

        Map<WeeklyTitle, Long> titles = resolver.resolve(List.of(alpha, charlie));

        assertThat(titles).containsExactly(Map.entry(WeeklyTitle.SCOUT, ALPHA.getId()));
    }

    private static WeeklyPlayerScore row(
        Player player,
        Integer position,
        int components,
        int food,
        int streakDays,
        int completedWeekly,
        int completedDaily
    ) {
        WeeklyPlayerScore score = RankingFixtures.score(player, position, food + components, 0);
        score.setComponents(components);
        score.setFood(food);
        score.setStreakDays(streakDays);
        score.setCompletedChallenges(completedWeekly);
        score.setCompletedDailyChallenges(completedDaily);

        return score;
    }
}
