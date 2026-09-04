package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Measures what the squad usually does in one match, anchor by anchor.
 *
 * <p>A median of medians, not an average of averages: one player who plays four times as much as
 * everyone else must not decide what "a normal game" looks like, and one thirty-kill deathmatch
 * must not decide it either. Each player is reduced to their own median first, and the squad's
 * anchor is the median of those.
 *
 * <p>Only talent is measured here. Volume — how much the squad plays — is the reference's job, and
 * mixing the two would make a challenge harder for a squad that simply plays more.
 */
final class SkillAnchorReader {

    /**
     * Modes a long-format anchor is measured on.
     */
    private static final List<GameMode> LONG_FORMAT_MODES =
        List.of(GameMode.COMPETITIVE, GameMode.UNRATED, GameMode.PREMIER);

    /**
     * Decimals a rate anchor keeps.
     */
    private static final int RATE_SCALE = 2;

    /**
     * Prevents instantiation of this utility.
     */
    private SkillAnchorReader() {
    }

    /**
     * Reduces one squad's matches to the anchors the challenge targets are resolved against.
     *
     * @param matchesByPlayer each roster player's matches over the calibration window
     * @return the anchors the squad measures, anchors nobody has any sample for omitted
     */
    static Map<SkillAnchor, BigDecimal> read(Map<Long, List<PlayerMatch>> matchesByPlayer) {
        Map<SkillAnchor, BigDecimal> anchors = new EnumMap<>(SkillAnchor.class);

        put(anchors, SkillAnchor.LONG_KILLS, matchesByPlayer, LONG_FORMAT_MODES, PlayerMatch::getKills, false);
        put(anchors, SkillAnchor.LONG_HEADSHOTS, matchesByPlayer, LONG_FORMAT_MODES,
            PlayerMatch::getHeadshots, false);
        put(anchors, SkillAnchor.LONG_ASSISTS, matchesByPlayer, LONG_FORMAT_MODES, PlayerMatch::getAssists, false);
        put(anchors, SkillAnchor.LONG_SCORE, matchesByPlayer, LONG_FORMAT_MODES, PlayerMatch::getScore, false);
        put(anchors, SkillAnchor.LONG_KD, matchesByPlayer, LONG_FORMAT_MODES, SkillAnchorReader::killDeathRatio, true);
        put(anchors, SkillAnchor.LONG_ADR, matchesByPlayer, LONG_FORMAT_MODES,
            match -> decimal(match.getAdr()), true);
        put(anchors, SkillAnchor.LONG_ACS, matchesByPlayer, LONG_FORMAT_MODES,
            match -> decimal(match.getAcs()), true);
        put(anchors, SkillAnchor.DEATHMATCH_KILLS, matchesByPlayer, List.of(GameMode.DEATHMATCH),
            PlayerMatch::getKills, false);
        put(anchors, SkillAnchor.DEATHMATCH_HEADSHOTS, matchesByPlayer, List.of(GameMode.DEATHMATCH),
            PlayerMatch::getHeadshots, false);
        put(anchors, SkillAnchor.TEAM_DEATHMATCH_KILLS, matchesByPlayer, List.of(GameMode.TEAM_DEATHMATCH),
            PlayerMatch::getKills, false);

        return anchors;
    }

    /**
     * Measures one anchor and stores it, unless nobody has a single sample of it.
     *
     * @param anchors         anchors gathered so far
     * @param anchor          anchor being measured
     * @param matchesByPlayer each roster player's matches
     * @param modes           modes the anchor is measured on
     * @param statistic       statistic read from one match
     * @param rate            whether the anchor keeps decimals
     */
    private static void put(
        Map<SkillAnchor, BigDecimal> anchors,
        SkillAnchor anchor,
        Map<Long, List<PlayerMatch>> matchesByPlayer,
        List<GameMode> modes,
        ToDoubleFunction<PlayerMatch> statistic,
        boolean rate
    ) {
        List<Double> playerMedians = new ArrayList<>(matchesByPlayer.size());

        for (List<PlayerMatch> matches : matchesByPlayer.values()) {
            List<Double> samples = matches.stream()
                .filter(match -> modes.contains(match.getMatch().getGameMode()))
                .map(statistic::applyAsDouble)
                .toList();

            if (!samples.isEmpty()) {
                playerMedians.add(median(samples));
            }
        }

        if (playerMedians.isEmpty()) {
            return;
        }

        double squadMedian = median(playerMedians);
        anchors.put(anchor, rate
            ? BigDecimal.valueOf(squadMedian).setScale(RATE_SCALE, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(Math.round(squadMedian)));
    }

    /**
     * Returns the median of a sample, averaging the two middle values of an even one.
     *
     * @param samples sample to reduce, never empty
     * @return the median
     */
    private static double median(List<Double> samples) {
        List<Double> sorted = new ArrayList<>(samples);
        sorted.sort(Double::compareTo);
        int middle = sorted.size() / 2;

        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }

        return (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    /**
     * Returns one match's kill-to-death ratio, a flawless match counting as its kills.
     *
     * @param match match to read
     * @return the ratio
     */
    private static double killDeathRatio(PlayerMatch match) {
        return match.getDeaths() == 0 ? match.getKills() : (double) match.getKills() / match.getDeaths();
    }

    /**
     * Reads one nullable decimal statistic.
     *
     * @param value stored statistic, {@code null} on a match Henrik reported without it
     * @return the value, zero when absent
     */
    private static double decimal(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }
}
