package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
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
 * Verifies what a week's validated challenges are worth in wounded, at the documented barème.
 *
 * <p>At a reference of 5 300 a first-week EASY brings back 5 and a VERY_HARD 29, and the daily
 * challenge 6.
 */
@ExtendWith(MockitoExtension.class)
class CampaignChallengeReaderTest {

    /**
     * Operators the campaign froze.
     */
    private static final Player ALPHA = CampaignFixtures.player(1, "Alpha");

    /**
     * Second frozen operator.
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
    @DisplayName("Prices each difficulty at the documented number of wounded")
    void shouldPriceEveryDifficulty() {
        stub(
            completed(ALPHA, weekly(ChallengeDifficulty.EASY, campaign.getFirstWeekStart())),
            completed(ALPHA, weekly(ChallengeDifficulty.VERY_HARD, campaign.getFirstWeekStart())),
            completed(BRAVO, daily(campaign.getFirstWeekStart()))
        );

        Map<Integer, WeekChallengeYield> yields = reader.read(campaign, Set.of(1L, 2L));

        assertThat(yields.get(1).survivors()).isEqualTo(5 + 29 + 6);
        assertThat(yields.get(1).survivorsByPlayer()).containsEntry(1L, 34).containsEntry(2L, 6);
        assertThat(yields.get(1).completionsByPlayer()).containsEntry(1L, 2).containsEntry(2L, 1);
    }

    @Test
    @DisplayName("Credits each validation to the week it was validated in")
    void shouldCreditEachWeekSeparately() {
        stub(
            completed(ALPHA, weekly(ChallengeDifficulty.EASY, campaign.getFirstWeekStart())),
            completed(ALPHA, weekly(ChallengeDifficulty.EASY, campaign.getFirstWeekStart().plusWeeks(2)))
        );

        Map<Integer, WeekChallengeYield> yields = reader.read(campaign, Set.of(1L));

        assertThat(yields).containsOnlyKeys(1, 3);
        // Week three pays 8 % more than week one: 5.30 x 1.08 rounds to 6 where week one gives 5.
        assertThat(yields.get(1).survivors()).isEqualTo(5);
        assertThat(yields.get(3).survivors()).isEqualTo(6);
    }

    @Test
    @DisplayName("Ignores a validation by someone the campaign never froze")
    void shouldIgnoreValidationsOutsideTheRoster() {
        stub(completed(OUTSIDER, weekly(ChallengeDifficulty.HARD, campaign.getFirstWeekStart())));

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
        when(progressRepository.findAllByCompletedTrueAndWeeklyChallengeWeekStartBetweenOrderByIdAsc(
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart()
        )).thenReturn(List.of(rows));
    }

    /**
     * Builds one validated progress row.
     *
     * @param player    operator who validated it
     * @param selection selection they validated
     * @return the progress row
     */
    private PlayerChallengeProgress completed(Player player, WeeklyChallenge selection) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(selection);
        progress.setCompleted(true);

        return progress;
    }

    /**
     * Builds one weekly selection.
     *
     * @param difficulty difficulty drawn
     * @param weekStart  Monday it belongs to
     * @return the selection
     */
    private WeeklyChallenge weekly(ChallengeDifficulty difficulty, LocalDate weekStart) {
        Challenge challenge = new Challenge();
        challenge.setCadence(ChallengeCadence.WEEKLY);
        challenge.setDifficulty(difficulty);

        WeeklyChallenge selection = new WeeklyChallenge();
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
    private WeeklyChallenge daily(LocalDate weekStart) {
        Challenge challenge = new Challenge();
        challenge.setCadence(ChallengeCadence.DAILY);
        challenge.setDifficulty(null);

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setChallenge(challenge);
        selection.setWeekStart(weekStart);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(weekStart.plusDays(2));

        return selection;
    }
}
