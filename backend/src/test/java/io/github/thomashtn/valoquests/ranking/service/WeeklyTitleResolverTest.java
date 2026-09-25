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
 * Verifies the four weekly honours, one per operator at most, and that a tie awards none of them.
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

    /**
     * Third ranked player.
     */
    private static final Player DELTA = RankingFixtures.player(4, "Delta", PlayerStatus.ACTIVE);

    /**
     * Fourth ranked player.
     */
    private static final Player ECHO = RankingFixtures.player(5, "Echo", PlayerStatus.ACTIVE);

    private final WeeklyTitleResolver resolver = new WeeklyTitleResolver();

    @Test
    @DisplayName("Awards each title to the single best figure of the week")
    void shouldAwardEachTitleToTheBest() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 900, 100, 3, 1, 0);
        WeeklyPlayerScore bravo = row(BRAVO, 2, 200, 800, 0, 0, 0);
        WeeklyPlayerScore delta = row(DELTA, 3, 0, 0, 5, 0, 0);
        WeeklyPlayerScore echo = row(ECHO, 4, 0, 0, 0, 2, 2);

        Map<WeeklyTitle, Long> titles = resolver.resolve(List.of(alpha, bravo, delta, echo), null);

        assertThat(titles).containsExactlyInAnyOrderEntriesOf(Map.of(
            WeeklyTitle.MECHANIC, ALPHA.getId(),
            WeeklyTitle.QUARTERMASTER, BRAVO.getId(),
            WeeklyTitle.REGULAR, DELTA.getId(),
            WeeklyTitle.SCOUT, ECHO.getId()
        ));
    }

    @Test
    @DisplayName("Gives an operator one title at most, passing the others to the next best figure")
    void shouldGiveOneTitleAtMostPerOperator() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 900, 800, 5, 2, 2);
        WeeklyPlayerScore bravo = row(BRAVO, 2, 200, 100, 3, 1, 0);

        Map<WeeklyTitle, Long> titles = resolver.resolve(List.of(alpha, bravo), null);

        assertThat(titles).containsExactlyInAnyOrderEntriesOf(Map.of(
            WeeklyTitle.MECHANIC, ALPHA.getId(),
            WeeklyTitle.QUARTERMASTER, BRAVO.getId()
        ));
    }

    @Test
    @DisplayName("Leaves the champion out, the runner-up on each figure taking the title")
    void shouldLeaveTheChampionOut() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 900, 800, 5, 2, 2);
        WeeklyPlayerScore bravo = row(BRAVO, 2, 200, 100, 3, 1, 0);
        WeeklyPlayerScore delta = row(DELTA, 3, 100, 300, 1, 0, 0);

        Map<WeeklyTitle, Long> titles = resolver.resolve(List.of(alpha, bravo, delta), ALPHA.getId());

        assertThat(titles).containsExactlyInAnyOrderEntriesOf(Map.of(
            WeeklyTitle.MECHANIC, BRAVO.getId(),
            WeeklyTitle.QUARTERMASTER, DELTA.getId()
        ));
    }

    @Test
    @DisplayName("Awards nothing on a tie, and nothing on a week where nobody produced")
    void shouldAwardNothingOnATieOrAnEmptyWeek() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 500, 0, 0, 0, 0);
        WeeklyPlayerScore bravo = row(BRAVO, 2, 500, 0, 0, 0, 0);

        assertThat(resolver.resolve(List.of(alpha, bravo), null)).isEmpty();
        assertThat(resolver.resolve(List.of(), null)).isEmpty();
    }

    @Test
    @DisplayName("Never awards a title to a row without a slot")
    void shouldIgnoreUnrankedRows() {
        WeeklyPlayerScore alpha = row(ALPHA, 1, 0, 0, 0, 1, 0);
        WeeklyPlayerScore charlie = row(CHARLIE, null, 0, 0, 0, 5, 3);

        Map<WeeklyTitle, Long> titles = resolver.resolve(List.of(alpha, charlie), null);

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
