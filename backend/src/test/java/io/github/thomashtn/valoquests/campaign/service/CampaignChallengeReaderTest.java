package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.scoring.service.DefaultScoringRuleset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies what a week's validated challenges are worth in wounded, at the documented scoring table.
 *
 * <p>At a reference of 5 300 a first-week EASY brings back 5, a VERY_HARD 29 and the daily challenge 6.
 */
@ExtendWith(MockitoExtension.class)
class CampaignChallengeReaderTest {

    /**
     * Players the campaign froze.
     */
    private static final Player ALPHA = CampaignFixtures.player(1, "Alpha");

    /**
     * Second frozen player.
     */
    private static final Player BRAVO = CampaignFixtures.player(2, "Bravo");

    /**
     * A player who joined after the campaign opened, and therefore pays nothing.
     */
    private static final Player OUTSIDER = CampaignFixtures.player(9, "Outsider");

    @Mock
    private PlayerChallengeProgressRepository progressRepository;

    private CampaignChallengeReader reader;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        reader = new CampaignChallengeReader(progressRepository, new DefaultScoringRuleset());
        campaign = CampaignFixtures.runningCampaign(1);
    }

    @Test
    @DisplayName("Prices each tier at the documented number of wounded")
    void shouldPriceEveryTier() {
        stub(
            completed(ALPHA, weekly(ChallengeTier.EASY, campaign.getFirstWeekStart())),
            completed(ALPHA, weekly(ChallengeTier.VERY_HARD, campaign.getFirstWeekStart())),
            completed(BRAVO, daily(campaign.getFirstWeekStart()))
        );

        Map<Integer, WeekChallengeYield> yields = reader.read(campaign, Set.of(1L, 2L));

        assertThat(yields.get(1).rescued()).isEqualTo(5 + 29 + 6);
    }

    @Test
    @DisplayName("Credits each validation to the week it was validated in")
    void shouldCreditEachWeekSeparately() {
        stub(
            completed(ALPHA, weekly(ChallengeTier.EASY, campaign.getFirstWeekStart())),
            completed(ALPHA, weekly(ChallengeTier.EASY, campaign.getFirstWeekStart().plusWeeks(2)))
        );

        Map<Integer, WeekChallengeYield> yields = reader.read(campaign, Set.of(1L));

        assertThat(yields).containsOnlyKeys(1, 3);
        // Week three pays 8 % more than week one: 5.30 x 1.08 rounds to 6 where week one gives 5.
        assertThat(yields.get(1).rescued()).isEqualTo(5);
        assertThat(yields.get(3).rescued()).isEqualTo(6);
    }

    @Test
    @DisplayName("Ignores a validation by someone the campaign never froze")
    void shouldIgnoreValidationsOutsideTheRoster() {
        stub(completed(OUTSIDER, weekly(ChallengeTier.HARD, campaign.getFirstWeekStart())));

        assertThat(reader.read(campaign, Set.of(1L, 2L))).isEmpty();
    }

    @Test
    @DisplayName("Reports nothing for a campaign nobody validated a challenge in")
    void shouldReportNothingWithoutValidations() {
        stub();

        assertThat(reader.read(campaign, Set.of(1L))).isEmpty();
    }

    /**
     * Stubs the repository with the validated rows of the campaign.
     *
     * @param rows validated rows
     */
    private void stub(PlayerChallengeProgress... rows) {
        when(progressRepository.findAllByCompletedTrueAndSelectionWeekStartBetweenOrderByIdAsc(
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart()
        )).thenReturn(List.of(rows));
    }

    /**
     * Builds one validated progress row.
     *
     * @param player    player who validated it
     * @param selection selection they validated
     * @return the progress row
     */
    private PlayerChallengeProgress completed(Player player, ChallengeSelection selection) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setSelection(selection);
        progress.setCompleted(true);

        return progress;
    }

    /**
     * Builds one weekly selection.
     *
     * @param tier tier drawn
     * @param weekStart  Monday it belongs to
     * @return the selection
     */
    private ChallengeSelection weekly(ChallengeTier tier, LocalDate weekStart) {
        Challenge challenge = new Challenge();
        challenge.setCadence(ChallengeCadence.WEEKLY);
        challenge.setTier(tier);

        ChallengeSelection selection = new ChallengeSelection();
        selection.setChallenge(challenge);
        selection.setWeekStart(weekStart);
        selection.setCadence(ChallengeCadence.WEEKLY);

        return selection;
    }

    /**
     * Builds one daily selection.
     *
     * @param weekStart Monday it belongs to
     * @return the selection
     */
    private ChallengeSelection daily(LocalDate weekStart) {
        Challenge challenge = new Challenge();
        challenge.setCadence(ChallengeCadence.DAILY);
        challenge.setTier(null);

        ChallengeSelection selection = new ChallengeSelection();
        selection.setChallenge(challenge);
        selection.setWeekStart(weekStart);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(weekStart.plusDays(2));

        return selection;
    }
}
