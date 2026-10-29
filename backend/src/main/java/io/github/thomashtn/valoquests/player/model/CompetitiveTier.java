package io.github.thomashtn.valoquests.player.model;

import java.util.Locale;

/**
 * Defines the competitive ranks supported by the application.
 *
 * <p>Declared from the lowest to the highest rank: comparisons rely on that order.
 */
public enum CompetitiveTier {
    /**
     * No competitive rank yet.
     */
    UNRANKED,
    /**
     * Lowest division of the Iron rank.
     */
    IRON_1,
    /**
     * Middle division of the Iron rank.
     */
    IRON_2,
    /**
     * Highest division of the Iron rank.
     */
    IRON_3,
    /**
     * Lowest division of the Bronze rank.
     */
    BRONZE_1,
    /**
     * Middle division of the Bronze rank.
     */
    BRONZE_2,
    /**
     * Highest division of the Bronze rank.
     */
    BRONZE_3,
    /**
     * Lowest division of the Silver rank.
     */
    SILVER_1,
    /**
     * Middle division of the Silver rank.
     */
    SILVER_2,
    /**
     * Highest division of the Silver rank.
     */
    SILVER_3,
    /**
     * Lowest division of the Gold rank.
     */
    GOLD_1,
    /**
     * Middle division of the Gold rank.
     */
    GOLD_2,
    /**
     * Highest division of the Gold rank.
     */
    GOLD_3,
    /**
     * Lowest division of the Platinum rank.
     */
    PLATINUM_1,
    /**
     * Middle division of the Platinum rank.
     */
    PLATINUM_2,
    /**
     * Highest division of the Platinum rank.
     */
    PLATINUM_3,
    /**
     * Lowest division of the Diamond rank.
     */
    DIAMOND_1,
    /**
     * Middle division of the Diamond rank.
     */
    DIAMOND_2,
    /**
     * Highest division of the Diamond rank.
     */
    DIAMOND_3,
    /**
     * Lowest division of the Ascendant rank.
     */
    ASCENDANT_1,
    /**
     * Middle division of the Ascendant rank.
     */
    ASCENDANT_2,
    /**
     * Highest division of the Ascendant rank.
     */
    ASCENDANT_3,
    /**
     * Lowest division of the Immortal rank.
     */
    IMMORTAL_1,
    /**
     * Middle division of the Immortal rank.
     */
    IMMORTAL_2,
    /**
     * Highest division of the Immortal rank.
     */
    IMMORTAL_3,
    /**
     * Highest rank of the game.
     */
    RADIANT;

    /**
     * Reads a Henrik tier name ("Gold 2"), unranked when absent or unknown.
     *
     * @param name Henrik tier name, may be {@code null}
     * @return matching tier
     */
    public static CompetitiveTier fromHenrikName(String name) {
        if (name == null || name.isBlank()) {
            return UNRANKED;
        }

        String normalized = name
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException _) {
            return UNRANKED;
        }
    }
}
