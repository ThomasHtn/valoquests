package io.github.thomashtn.valoquests.campaign.model;

/**
 * Bracket a campaign's reference falls in, so two squads of very different levels can compare runs.
 *
 * <p>Every figure of the game is a multiple of the reference, so a base of 30 000 and a base of
 * 119 000 describe the same performance at two levels. The tier is what makes that readable: it is
 * printed next to the score and never enters a single formula.
 */
public enum CampaignTier {

    /**
     * Under 3 500 a week per player: about four competitive games and three quick ones.
     */
    AMATEUR(3_500),

    /**
     * 3 500 to 9 000: about seven competitive games and nine quick ones.
     */
    NORMAL(9_000),

    /**
     * 9 000 to 16 000: about sixteen competitive games and twenty-five quick ones.
     */
    CONFIRMED(16_000),

    /**
     * Above 16 000: about twenty-eight competitive games and thirty-five quick ones.
     */
    ELITE(Integer.MAX_VALUE);

    /**
     * Weekly reference per player this tier stops at, exclusive.
     */
    private final int exclusiveCeiling;

    /**
     * Creates a tier.
     *
     * @param exclusiveCeiling weekly reference per player the tier stops at, exclusive
     */
    CampaignTier(int exclusiveCeiling) {
        this.exclusiveCeiling = exclusiveCeiling;
    }

    /**
     * Places a weekly reference on the ladder.
     *
     * @param reference weekly reference per player
     * @return the tier the reference falls in
     */
    public static CampaignTier of(int reference) {
        for (CampaignTier tier : values()) {
            if (reference < tier.exclusiveCeiling) {
                return tier;
            }
        }

        return ELITE;
    }
}
