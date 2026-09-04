package io.github.thomashtn.valoquests.challenge.model;

import java.util.Optional;

/**
 * Identifies one squad talent anchor: a per-match statistic measured on one family of modes.
 *
 * <p>The calibration reference measures volume, never talent: a match is worth the same damage
 * whether it was won with five kills or thirty. Per-match bars and rates therefore scale on these
 * anchors, the squad's median of each player's median over the calibration window, and never on
 * the reference.
 */
public enum SkillAnchor {

    /**
     * Kills per competitive or unrated match.
     */
    LONG_KILLS,

    /**
     * Headshots per competitive or unrated match.
     */
    LONG_HEADSHOTS,

    /**
     * Assists per competitive or unrated match.
     */
    LONG_ASSISTS,

    /**
     * Combat score per competitive or unrated match.
     */
    LONG_SCORE,

    /**
     * Kill-to-death ratio on competitive or unrated matches.
     */
    LONG_KD,

    /**
     * Average damage per round on competitive or unrated matches.
     */
    LONG_ADR,

    /**
     * Average combat score per round on competitive or unrated matches.
     */
    LONG_ACS,

    /**
     * Kills per deathmatch.
     */
    DEATHMATCH_KILLS,

    /**
     * Headshots per deathmatch.
     */
    DEATHMATCH_HEADSHOTS,

    /**
     * Kills per team deathmatch.
     */
    TEAM_DEATHMATCH_KILLS;

    /**
     * Finds the anchor a per-match condition scales on.
     *
     * @param metric   statistic the condition measures per match
     * @param gameMode filter the condition declares
     * @return the matching anchor, empty when the squad measures no such thing
     */
    public static Optional<SkillAnchor> of(ChallengeMetric metric, ChallengeGameMode gameMode) {
        SkillAnchor anchor = switch (gameMode) {
            case COMPETITIVE, UNRATED, COMPETITIVE_OR_UNRATED -> longFormat(metric);
            case DEATHMATCH -> deathmatch(metric);
            case TEAM_DEATHMATCH -> metric == ChallengeMetric.KILLS ? TEAM_DEATHMATCH_KILLS : null;
            case ANY -> null;
        };

        return Optional.ofNullable(anchor);
    }

    /**
     * Finds the deathmatch anchor of one metric.
     *
     * @param metric statistic measured per match
     * @return the anchor, or {@code null} when none exists
     */
    private static SkillAnchor deathmatch(ChallengeMetric metric) {
        return switch (metric) {
            case KILLS -> DEATHMATCH_KILLS;
            case HEADSHOTS -> DEATHMATCH_HEADSHOTS;
            default -> null;
        };
    }

    /**
     * Finds the long-format anchor of one metric.
     *
     * @param metric statistic measured per match
     * @return the anchor, or {@code null} when none exists
     */
    private static SkillAnchor longFormat(ChallengeMetric metric) {
        return switch (metric) {
            case KILLS -> LONG_KILLS;
            case HEADSHOTS -> LONG_HEADSHOTS;
            case ASSISTS -> LONG_ASSISTS;
            case SCORE -> LONG_SCORE;
            case KD -> LONG_KD;
            case ADR -> LONG_ADR;
            case ACS -> LONG_ACS;
            default -> null;
        };
    }
}
