package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.service.ChallengePointsReader.ChallengeTally;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.scoring.service.DefaultScoringRuleset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies how validated challenges are priced and tallied for the ranking.
 */
@ExtendWith(MockitoExtension.class)
class ChallengePointsReaderTest {

    /**
     * Monday of the week being priced.
     */
    private static final LocalDate WEEK_START = RankingFixtures.WEEK_START;

    /**
     * Reference the calibration source answers with.
     */
    private static final int REFERENCE = 5_300;

    /**
     * Player validating challenges.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Player validating nothing.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    @Mock
    private PlayerChallengeProgressRepository progressRepository;

    private ChallengePointsReader reader;

    @BeforeEach
    void setUp() {
        reader = new ChallengePointsReader(
            progressRepository,
            new DefaultScoringRuleset(),
            weekStart -> new ChallengeCalibration(REFERENCE, 3, CampaignDifficulty.AMATEUR)
        );
    }

    @Test
    @DisplayName("Tallies each player's validations, daily and weekly counted apart")
    void shouldTallyValidationsPerPlayer() {
        when(progressRepository.findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(WEEK_START))
            .thenReturn(List.of(
                progress(ALPHA, selection(1, ChallengeCadence.WEEKLY, ChallengeTier.EASY), true),
                progress(ALPHA, selection(2, ChallengeCadence.WEEKLY, ChallengeTier.HARD), true),
                progress(ALPHA, selection(3, ChallengeCadence.DAILY, null), true),
                progress(ALPHA, selection(4, ChallengeCadence.WEEKLY, ChallengeTier.VERY_HARD), false),
                progress(BRAVO, selection(1, ChallengeCadence.WEEKLY, ChallengeTier.EASY), false)
            ));

        Map<Long, ChallengeTally> tallies = reader.read(WEEK_START);

        // Week three: EASY 6 + HARD 22 + daily 7; the incomplete VERY_HARD pays nothing.
        assertThat(tallies).containsOnlyKeys(ALPHA.getId());
        assertThat(tallies.get(ALPHA.getId())).isEqualTo(new ChallengeTally(35, 2, 1));
    }

    @Test
    @DisplayName("Prices a challenge at the cadence it was drawn with, whatever the catalogue says today")
    void shouldPriceAtTheDrawnCadence() {
        ChallengeSelection drawnDaily = selection(1, ChallengeCadence.DAILY, ChallengeTier.VERY_HARD);
        drawnDaily.getChallenge().setCadence(ChallengeCadence.WEEKLY);
        when(progressRepository.findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(WEEK_START))
            .thenReturn(List.of(progress(ALPHA, drawnDaily, true)));

        // Daily weight 1.2 at week three: 5 300 x 1.2 / 1 000 x 1.08, rounded.
        assertThat(reader.read(WEEK_START).get(ALPHA.getId())).isEqualTo(new ChallengeTally(7, 0, 1));
    }

    private static ChallengeSelection selection(long id, ChallengeCadence cadence, ChallengeTier tier) {
        Challenge challenge = new Challenge();
        challenge.setCadence(cadence);
        challenge.setTier(tier);

        ChallengeSelection selection = new ChallengeSelection();
        selection.setId(id);
        selection.setWeekStart(WEEK_START);
        selection.setCadence(cadence);
        selection.setChallenge(challenge);

        return selection;
    }

    private static PlayerChallengeProgress progress(Player player, ChallengeSelection selection, boolean completed) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setSelection(selection);
        progress.setCompleted(completed);

        return progress;
    }
}
