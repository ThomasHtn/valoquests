package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices what a campaign's validated challenges bring back, week by week.
 *
 * <p>A challenge never damages a guardian: it rescues wounded, and those wounded are acquired
 * whatever else happens that week. They leave first on Sunday, spending neither food nor
 * components and without suffering the guardian progress — the operators went and got them.
 *
 * <p>Both cadences are read the same way. The daily challenge resolves on its own evening but its
 * rescues wait for the ship like everyone else's, so it is credited to the week it falls in.
 */
@Service
@Transactional(readOnly = true)
public class CampaignChallengeReader {

    /**
     * Repository used to read every validated challenge of the campaign in one query.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Barème pricing one validated challenge in wounded.
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
            .findAllByCompletedTrueAndWeeklyChallengeWeekStartBetweenOrderByIdAsc(
                campaign.getFirstWeekStart(),
                campaign.getLastWeekStart()
            );

        Map<Integer, Integer> totals = new HashMap<>();
        Map<Integer, Map<Long, Integer>> survivorsByPlayer = new HashMap<>();
        Map<Integer, Map<Long, Integer>> completionsByPlayer = new HashMap<>();

        for (PlayerChallengeProgress progress : completed) {
            long playerId = progress.getPlayer().getId();

            if (!rosterIdentifiers.contains(playerId)) {
                continue;
            }

            WeeklyChallenge selection = progress.getWeeklyChallenge();
            int weekIndex = campaign.weekIndexOf(selection.getWeekStart());
            int survivors = survivorsOf(selection, campaign.getReference(), weekIndex);

            totals.merge(weekIndex, survivors, Integer::sum);
            survivorsByPlayer.computeIfAbsent(weekIndex, ignored -> new HashMap<>())
                .merge(playerId, survivors, Integer::sum);
            completionsByPlayer.computeIfAbsent(weekIndex, ignored -> new HashMap<>())
                .merge(playerId, 1, Integer::sum);
        }

        Map<Integer, WeekChallengeYield> yields = new HashMap<>(totals.size());
        totals.forEach((weekIndex, survivors) -> yields.put(weekIndex, new WeekChallengeYield(
            survivors,
            survivorsByPlayer.getOrDefault(weekIndex, Map.of()),
            completionsByPlayer.getOrDefault(weekIndex, Map.of())
        )));

        return yields;
    }

    /**
     * Prices one validated selection in wounded.
     *
     * @param selection selection the operator validated
     * @param reference campaign reference
     * @param weekIndex one-based week the selection belongs to
     * @return the wounded it brings back
     */
    private int survivorsOf(WeeklyChallenge selection, int reference, int weekIndex) {
        double weight = ruleset.challengeWeight(
            selection.getChallenge().getCadence(),
            selection.getChallenge().getDifficulty()
        );

        return ruleset.challengeSurvivors(reference, weight, weekIndex);
    }
}
