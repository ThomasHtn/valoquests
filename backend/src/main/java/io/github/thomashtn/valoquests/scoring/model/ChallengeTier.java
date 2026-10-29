package io.github.thomashtn.valoquests.scoring.model;

/**
 * Grades a weekly challenge inside its pack, from the easiest to the hardest slot.
 *
 * <p>Not to be confused with {@link CampaignDifficulty}, the setting a whole campaign is played at.
 * Declaration order is the pack order, easiest first: sorting relies on it.
 */
public enum ChallengeTier {
    /**
     * Easiest slot of the pack.
     */
    EASY,

    /**
     * Second slot, a little above easy.
     */
    NORMAL,

    /**
     * Middle slot of the pack.
     */
    MEDIUM,

    /**
     * Demanding slot, reached by good players only.
     */
    HARD,

    /**
     * Hardest slot of the pack.
     */
    VERY_HARD
}
