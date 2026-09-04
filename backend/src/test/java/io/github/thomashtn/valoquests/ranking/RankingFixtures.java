package io.github.thomashtn.valoquests.ranking;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builders shared by the ranking tests.
 */
public final class RankingFixtures {

    /**
     * Monday of the week the fixtures live in.
     */
    public static final LocalDate WEEK_START = LocalDate.of(2026, 9, 7);

    /**
     * Instant inside that week.
     */
    public static final Instant MIDWEEK = Instant.parse("2026-09-09T12:00:00Z");

    private RankingFixtures() {
    }

    /**
     * Creates one player.
     *
     * @param id     identifier
     * @param name   Riot name, also the display name
     * @param status status
     * @return the player
     */
    public static Player player(long id, String name, PlayerStatus status) {
        Player player = new Player();
        player.setId(id);
        player.setGameName(name);
        player.setTagLine("EUW");
        player.setDisplayName(name);
        player.setPortrait("portraits/" + name.toLowerCase() + ".png");
        player.setStatus(status);

        return player;
    }

    /**
     * Creates one ranked row.
     *
     * @param player          player the row belongs to
     * @param position        position, {@code null} for an unranked row
     * @param guardianDamage  guardian damage
     * @param challengePoints challenge points
     * @return the row, with its total set
     */
    public static WeeklyPlayerScore score(Player player, Integer position, int guardianDamage, int challengePoints) {
        WeeklyPlayerScore score = new WeeklyPlayerScore();
        score.setId(player.getId() * 100);
        score.setPlayer(player);
        score.setWeekStart(WEEK_START);
        score.setPosition(position);
        score.setGuardianDamage(guardianDamage);
        score.setChallengePoints(challengePoints);
        score.setTotalPoints(guardianDamage + challengePoints);
        score.setCalculatedAt(MIDWEEK);

        return score;
    }

    /**
     * Creates one day's output.
     *
     * @param damage     damage, split 30/70 into food and components
     * @param matchCount matches played
     * @param streakDays streak the day sits at
     * @return the output
     */
    public static PlayerDayOutput dayOutput(int damage, int matchCount, int streakDays) {
        int food = damage * 3 / 10;

        return new PlayerDayOutput(damage, food, damage - food, matchCount, 0, streakDays, 0);
    }

    /**
     * Builds a reading from per-player, per-day outputs; the streak of each day is taken from the
     * output itself.
     *
     * @param outputs output per player and per day
     * @return the reading
     */
    public static DailyOutput output(Map<Long, Map<LocalDate, PlayerDayOutput>> outputs) {
        Map<LocalDate, Map<Long, PlayerDayOutput>> byDay = new HashMap<>();
        Map<Long, Map<LocalDate, Integer>> streaks = new HashMap<>();

        outputs.forEach((playerId, days) -> days.forEach((day, output) -> {
            byDay.computeIfAbsent(day, ignored -> new HashMap<>()).put(playerId, output);
            streaks.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(day, output.streakDays());
        }));

        return new DailyOutput(byDay, streaks, List.of());
    }
}
