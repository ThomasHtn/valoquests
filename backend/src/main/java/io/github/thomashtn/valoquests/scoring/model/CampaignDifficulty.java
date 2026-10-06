package io.github.thomashtn.valoquests.scoring.model;

/**
 * Difficulty a campaign is played at, chosen by the admin at opening and frozen with it.
 *
 * <p>Picks which written challenge target is served and the reference every campaign figure scales with.
 * Lives here because the challenge catalogue reads it and must not depend on the campaign.
 */
public enum CampaignDifficulty {

    /**
     * For a squad playing regularly, and the default of a new campaign.
     *
     * <p>5 300 is the reference the catalogue's targets are written at, so they are served untouched.
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
