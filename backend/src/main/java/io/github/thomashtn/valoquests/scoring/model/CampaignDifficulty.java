package io.github.thomashtn.valoquests.scoring.model;

/**
 * Difficulty a campaign is played at, chosen by the admin at opening and frozen with it.
 *
 * <p>The single dial of the game. It decides two things at once: which of a challenge's two written
 * targets the catalogue serves, and the reference every figure of the campaign is a multiple of —
 * the guardian's hit points, the group of wounded, the survivors a challenge rescues and the points
 * it is worth.
 *
 * <p>Nothing is derived from match history: two numbers, written here, picked once, so the same
 * challenge shows the same target to every squad at the same difficulty.
 *
 * <p>Sits in this package rather than with the campaign because the challenge catalogue reads it and
 * must not depend on the campaign.
 */
public enum CampaignDifficulty {

    /**
     * For a squad playing regularly, and the default of a new campaign.
     *
     * <p>5 300 is the reference the challenge catalogue's targets are written at, so its numbers are
     * served untouched.
     */
    AMATEUR(5_300),

    /**
     * For a squad that clears the amateur campaign without effort: twice the guardian, and the harder of
     * the two written targets.
     */
    PRO(10_600);

    /**
     * Weekly reference per player the campaign is sized on.
     */
    private final int reference;

    /**
     * Creates a difficulty.
     *
     * @param reference weekly reference per player
     */
    CampaignDifficulty(int reference) {
        this.reference = reference;
    }

    /**
     * Returns the weekly reference per player this difficulty plays at.
     *
     * @return the reference
     */
    public int reference() {
        return reference;
    }
}
