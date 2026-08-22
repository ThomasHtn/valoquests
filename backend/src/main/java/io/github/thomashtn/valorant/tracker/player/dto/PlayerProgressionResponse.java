package io.github.thomashtn.valorant.tracker.player.dto;

import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;

/**
 * Exposes everything the player profile's "progression" view renders.
 *
 * <p>One payload rather than six endpoints: every section reads the same filtered set of matches,
 * so splitting them would make the frontend fetch and the backend re-aggregate the same history
 * several times over for a single screen.
 *
 * <p>Every figure here is scoped to competitive matches, the only queue whose combat score, damage
 * per round and win rate are comparable across matches. The one deliberate exception is
 * {@link PersonalRecords#longestActiveDayStreak()}, which counts days a player showed up at all.
 *
 * @param evolution per-season match-by-match series, one entry per selected season
 * @param aim       where the player's hits land, over the whole filtered set
 * @param weekdays  win rate per day of the week, always seven entries, Monday first
 * @param hourSlots win rate per three-hour slot, always eight entries, starting at midnight
 * @param records   the player's personal bests over the filtered set
 * @param maps      aggregated statistics per map, most-played first
 * @param agents    aggregated statistics per agent, most-played first
 */
@Schema(description = "Aggregated analytics backing the player profile's progression view.")
public record PlayerProgressionResponse(

    List<SeasonEvolution> evolution,
    AimBreakdown aim,
    List<WeekdayPerformance> weekdays,
    List<HourSlotPerformance> hourSlots,
    PersonalRecords records,
    List<MapStatisticsResponse> maps,
    List<AgentStatisticsResponse> agents
) {

    /**
     * Exposes one season's match-by-match progression.
     *
     * @param seasonId   internal season identifier
     * @param seasonName human-readable season name
     * @param active     whether this is the season currently in progress
     * @param points     one point per match, oldest first
     * @param averages   the season's mean value for each plotted metric
     */
    public record SeasonEvolution(

        Long seasonId,
        String seasonName,
        boolean active,
        List<MatchPoint> points,
        Averages averages
    ) {
        /**
         * Creates an immutable season-evolution entry.
         */
        public SeasonEvolution {
            points = List.copyOf(points);
        }
    }

    /**
     * Exposes one match as a point on the evolution charts.
     *
     * @param startedAt          when the match started, in UTC
     * @param headshotPercentage share of that match's hits that landed on the head
     * @param kda                that match's ratio of kills and assists to deaths
     * @param acs                that match's average combat score, or {@code null} when unreported
     * @param adr                that match's average damage per round, or {@code null} when unreported
     */
    public record MatchPoint(

        Instant startedAt,
        BigDecimal headshotPercentage,
        BigDecimal kda,
        BigDecimal acs,
        BigDecimal adr
    ) {
    }

    /**
     * Exposes a season's mean value for each metric the evolution charts plot.
     *
     * <p>Computed here rather than in the browser so the legend and the curve can never disagree.
     *
     * @param headshotPercentage mean headshot rate
     * @param kda                mean ratio of kills and assists to deaths
     * @param acs                mean average combat score
     * @param adr                mean average damage per round
     */
    public record Averages(

        BigDecimal headshotPercentage,
        BigDecimal kda,
        BigDecimal acs,
        BigDecimal adr
    ) {
    }

    /**
     * Exposes where a player's hits land.
     *
     * <p>Riot reports these as per-match totals, never per round, so this is a share of registered
     * hits and not an accuracy figure: it says nothing about the shots that missed entirely.
     *
     * @param headPercentage share of hits on the head
     * @param bodyPercentage share of hits on the body
     * @param legPercentage  share of hits on the legs
     * @param totalShots     hits the shares are computed from, so a caller can judge the sample
     */
    public record AimBreakdown(

        BigDecimal headPercentage,
        BigDecimal bodyPercentage,
        BigDecimal legPercentage,
        long totalShots
    ) {
    }

    /**
     * Exposes one day of the week's performance.
     *
     * @param day           the day being summarized
     * @param matchesPlayed matches played on that day, across the filtered set
     * @param wins          matches won on that day
     * @param winRate       share of matches won, as a percentage
     * @param best          whether this is the player's strongest day; see {@link HourSlotPerformance#best()}
     */
    public record WeekdayPerformance(

        DayOfWeek day,
        long matchesPlayed,
        long wins,
        BigDecimal winRate,
        boolean best
    ) {
    }

    /**
     * Exposes one three-hour slot's performance.
     *
     * @param startHour     first hour of the slot, in the application's calendar zone
     * @param matchesPlayed matches started within that slot
     * @param wins          matches won within that slot
     * @param winRate       share of matches won, as a percentage
     * @param best          whether this is the player's strongest slot. Only a slot with enough
     *     matches behind it can be flagged, so a lone lucky win never crowns a time of day; at most
     *     one slot carries the flag, and none does when no slot clears the sample threshold
     */
    public record HourSlotPerformance(

        int startHour,
        long matchesPlayed,
        long wins,
        BigDecimal winRate,
        boolean best
    ) {
    }

    /**
     * Exposes a player's personal bests.
     *
     * <p>Deliberately all-time highs and never lows: this section is read by the player it
     * describes, and a "worst match" tile would only be a place to feel bad about.
     *
     * @param mostKills                best kill count in a single match
     * @param bestAcs                  best average combat score in a single match
     * @param mostDamage               most damage dealt in a single match
     * @param bestKda                  best ratio of kills and assists to deaths in a single match
     * @param bestHeadshotPercentage   best headshot rate in a single match, over a long enough match
     *     that the figure means something
     * @param longestWinStreak         longest run of consecutive wins
     * @param longestActiveDayStreak   longest run of consecutive days with at least one match, in
     *     any game mode
     * @param mvps                     matches finished with the best score of the game
     * @param peakTier                 highest competitive rank reached, or {@code null} when unranked
     */
    public record PersonalRecords(

        RecordEntry mostKills,
        RecordEntry bestAcs,
        RecordEntry mostDamage,
        RecordEntry bestKda,
        RecordEntry bestHeadshotPercentage,
        int longestWinStreak,
        int longestActiveDayStreak,
        long mvps,
        CompetitiveTier peakTier
    ) {
    }

    /**
     * Exposes one personal best and the match it was set in.
     *
     * <p>The whole entry is {@code null} when no match qualifies, rather than a zero-valued one: a
     * record of zero kills is not a record.
     *
     * @param value      the record figure itself
     * @param achievedAt when the match was played, in UTC
     * @param mapName    map the record was set on
     * @param agentName  agent the record was set with
     */
    public record RecordEntry(

        BigDecimal value,
        Instant achievedAt,
        String mapName,
        String agentName
    ) {
    }

    /**
     * Creates an immutable progression response.
     */
    public PlayerProgressionResponse {
        evolution = List.copyOf(evolution);
        weekdays = List.copyOf(weekdays);
        hourSlots = List.copyOf(hourSlots);
        maps = List.copyOf(maps);
        agents = List.copyOf(agents);
    }
}
