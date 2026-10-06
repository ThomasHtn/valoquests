package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.service.ScoringRuleset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices what a campaign's validated challenges bring back, week by week.
 *
 * <p>Challenge rescues are acquired whatever happens: they cost no stock and ignore guardian progress.
 * Daily challenges are credited to the week they fall in.
 */
@Service
@Transactional(readOnly = true)
public class CampaignChallengeReader {

    /**
     * Repository used to read every validated challenge of the campaign in one query.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Scoring table pricing one validated challenge in wounded.
     */
    private final ScoringRuleset ruleset;

    /**
     * Creates the campaign challenge reader.
     *
     * @param progressRepository player challenge progress repository
     * @param ruleset            scoring ruleset
     */
    public CampaignChallengeReader(PlayerChallengeProgressRepository progressRepository, ScoringRuleset ruleset) {
        this.progressRepository = progressRepository;
        this.ruleset = ruleset;
    }

    /**
     * Reads what each week of one campaign brought back.
     *
     * @param campaign campaign to read
     * @param rosterIdentifiers players frozen into the campaign's roster
     * @return the yield per one-based week index, weeks without a validation omitted
     */
    public Map<Integer, WeekChallengeYield> read(Campaign campaign, Set<Long> rosterIdentifiers) {
        List<PlayerChallengeProgress> completed = progressRepository
            .findAllByCompletedTrueAndSelectionWeekStartBetweenOrderByIdAsc(
                campaign.getFirstWeekStart(),
                campaign.getLastWeekStart()
            );

        Map<Integer, Integer> totals = new HashMap<>();

        for (PlayerChallengeProgress progress : completed) {
            if (!rosterIdentifiers.contains(progress.getPlayer().getId())) {
                continue;
            }

            ChallengeSelection selection = progress.getSelection();
            int weekIndex = campaign.weekIndexOf(selection.getWeekStart());

            ChallengeCalibration calibration =
                new ChallengeCalibration(campaign.reference(), weekIndex, campaign.getDifficulty());
            int rescued = ruleset.challengeReward(
                selection.getCadence(),
                selection.getChallenge().getTier(),
                calibration
            );

            totals.merge(weekIndex, rescued, Integer::sum);
        }

        Map<Integer, WeekChallengeYield> yields = new HashMap<>(totals.size());
        totals.forEach((weekIndex, rescued) -> yields.put(weekIndex, new WeekChallengeYield(rescued)));

        return yields;
    }
}
