package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDayActivity;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.model.DailyMatchDamage;
import io.github.thomashtn.valoquests.scoring.service.DailyMatchDamageReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads what the squad did, day by day, over a stretch of the calendar.
 *
 * <p>Both readings come from {@link DailyMatchDamageReader}, which prices a day once for everything
 * that reads one: the colony turns a day into food, the leaderboard's day scope ranks the same day, and
 * the two must not publish different figures for one evening. What this class adds on top is the
 * colony's own reading of it — the turnout threshold, and the day shape the replay consumes.
 *
 * <p>Only players holding {@link Player#COMPETITIVE_STATUS} count. This used to read every match
 * whatever the player's status, on the argument that a numerator ignoring the roster is stable across
 * a deactivation and therefore keeps the replay pure. It bought that purity at the price of a town no
 * gauge could account for: a deactivated player kept bringing food in while appearing on none of the
 * roster the turnout rail draws, so an evening's harvest had no author anywhere in the interface, and
 * an operator who had deliberately taken somebody off the roster went on being fed by them.
 *
 * <p>The status is the one the player holds now, so deactivating somebody rewrites the run's past days
 * on the next replay. That is the intended reading — the roster is a statement about who is in the
 * campaign, not only about who is in it today — and it is what the turnout readout has always done,
 * having named the current roster on every day it draws since it was written.
 */
@Service
@Transactional(readOnly = true)
public class ColonyActivityReader {

    /**
     * Reader pricing the roster's matches day by day, shared with everything else that reads a day.
     */
    private final DailyMatchDamageReader damageReader;

    /**
     * Ruleset supplying the raw damage a day must clear to count towards turnout.
     */
    private final ColonyRuleset colonyRuleset;

    /**
     * Creates the activity reader.
     *
     * @param damageReader  daily match damage reader
     * @param colonyRuleset colony ruleset
     */
    public ColonyActivityReader(
        DailyMatchDamageReader damageReader,
        ColonyRuleset colonyRuleset
    ) {
        this.damageReader = damageReader;
        this.colonyRuleset = colonyRuleset;
    }

    /**
     * Reads every day of an inclusive range on which at least one match was played.
     *
     * <p>Walks whole weeks because that is the unit the resolver ranks matches in. A day absent from the
     * result is a day nobody played.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return activity indexed by day, days without a match omitted
     */
    public Map<LocalDate, ColonyDayActivity> readActivity(LocalDate firstDay, LocalDate lastDay) {
        DailyMatchDamage reading = read(firstDay, lastDay);
        Map<LocalDate, ColonyDayActivity> activityByDay = new HashMap<>();

        reading.weightedDamageByDay().forEach((day, weightedDamage) -> {
            if (day.isBefore(firstDay) || day.isAfter(lastDay)) {
                return;
            }

            activityByDay.put(
                day,
                new ColonyDayActivity(weightedDamage, presenceCount(reading.rawDamageOn(day)))
            );
        });

        return activityByDay;
    }

    /**
     * Reads what each player brought to one day, before the daily diminishing returns.
     *
     * <p>Feeds the turnout readout, which shows every player of the roster whether they cleared the
     * threshold, fell short of it, or did not play at all. Players absent from the result played
     * nothing.
     *
     * @param day day to read
     * @return raw damage indexed by player identifier, players who did not play omitted
     */
    public Map<Long, Integer> readRawDamageByPlayer(LocalDate day) {
        return read(day, day).rawDamageOn(day);
    }

    /**
     * Returns how many players of a day cleared the turnout threshold.
     *
     * @param rawDamageByPlayer raw damage of the day, indexed by player identifier
     * @return players counting towards turnout
     */
    public int presenceCount(Map<Long, Integer> rawDamageByPlayer) {
        return (int) rawDamageByPlayer.values().stream()
            .filter(rawDamage -> rawDamage >= colonyRuleset.presenceDamageThreshold())
            .count();
    }

    /**
     * Reads a range through the shared daily reader, on the roster the colony accounts for.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return both readings, keyed by day
     */
    private DailyMatchDamage read(LocalDate firstDay, LocalDate lastDay) {
        return damageReader.read(Set.of(Player.COMPETITIVE_STATUS), firstDay, lastDay);
    }
}
