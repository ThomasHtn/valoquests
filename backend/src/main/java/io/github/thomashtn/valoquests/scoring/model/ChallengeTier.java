package io.github.thomashtn.valoquests.scoring.model;

/**
 * Grades a weekly challenge inside its pack, from the easiest to the hardest slot.
 *
 * <p>Not to be confused with {@link CampaignDifficulty}, the setting a whole campaign is played at.
 * Declaration order is the pack order, easiest first: sorting relies on it.
 */
public enum ChallengeTier {
    EASY, NORMAL, MEDIUM, HARD, VERY_HARD
}
