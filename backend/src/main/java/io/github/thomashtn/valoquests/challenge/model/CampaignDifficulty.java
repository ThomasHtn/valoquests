package io.github.thomashtn.valoquests.challenge.model;

/**
 * Difficulty a campaign is played at, chosen by the operator at opening and frozen with it.
 *
 * <p>The single dial of the game. It decides two things at once: which of a challenge's two written
 * targets the catalogue serves, and the reference every figure of the campaign is a multiple of —
 * the guardian's hit points, the group of wounded, the survivors a challenge rescues and the points
 * it is worth.
 *
 * <p>Replaces the reference measured on match history. That measure counted empty weeks as zeros, so
 * a squad whose window had not been imported landed on a floor and beat its guardian by Wednesday;
 * and it made the catalogue unreadable, the same challenge showing twelve kills to one squad and
 * thirty to another. Nothing is derived any more: two numbers, written here, picked once.
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
     * For a squad that clears the amateur run without effort: twice the guardian, and the harder of
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
