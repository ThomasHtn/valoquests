package io.github.thomashtn.valoquests.campaign.model;

/**
 * What one week's challenges brought back, before the group caps it.
 *
 * @param rescued wounded the whole roster's validated challenges brought back
 */
public record WeekChallengeYield(int rescued) {

    /**
     * A week whose challenges nobody validated.
     */
    public static final WeekChallengeYield NONE = new WeekChallengeYield(0);
}
