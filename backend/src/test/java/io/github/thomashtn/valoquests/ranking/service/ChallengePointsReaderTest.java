package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.service.ChallengePointsReader.ChallengeTally;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
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
            weekStart -> new ChallengeCalibration(REFERENCE, 3, ChallengeScaling.NONE)
        );
    }

    @Test
    @DisplayName("Prices the document's figures: 64 for the daily, then 53 to 286 by difficulty")
    void shouldPriceTheDocumentsFigures() {
        assertThat(reader.pointsOf(selection(1, ChallengeCadence.DAILY, null), REFERENCE)).isEqualTo(64);
        assertThat(reader.pointsOf(selection(2, ChallengeCadence.WEEKLY, ChallengeDifficulty.EASY), REFERENCE))
            .isEqualTo(53);
        assertThat(reader.pointsOf(selection(3, ChallengeCadence.WEEKLY, ChallengeDifficulty.VERY_HARD), REFERENCE))
            .isEqualTo(286);
    }

    @Test
    @DisplayName("Tallies each player's validations, daily and weekly counted apart")
    void shouldTallyValidationsPerPlayer() {
        when(progressRepository.findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of(
                progress(ALPHA, selection(1, ChallengeCadence.WEEKLY, ChallengeDifficulty.EASY), true),
                progress(ALPHA, selection(2, ChallengeCadence.WEEKLY, ChallengeDifficulty.HARD), true),
                progress(ALPHA, selection(3, ChallengeCadence.DAILY, null), true),
                progress(ALPHA, selection(4, ChallengeCadence.WEEKLY, ChallengeDifficulty.VERY_HARD), false),
                progress(BRAVO, selection(1, ChallengeCadence.WEEKLY, ChallengeDifficulty.EASY), false)
            ));

        Map<Long, ChallengeTally> tallies = reader.read(WEEK_START);

        // EASY 53 + HARD 207 + daily 64: the incomplete VERY_HARD pays nothing.
        assertThat(tallies).containsOnlyKeys(ALPHA.getId());
        assertThat(tallies.get(ALPHA.getId())).isEqualTo(new ChallengeTally(324, 2, 1));
    }

    @Test
    @DisplayName("Reads the reference in force from the calibration source")
    void shouldReadTheReferenceInForce() {
        assertThat(reader.referenceFor(WEEK_START)).isEqualTo(REFERENCE);
    }

    private static WeeklyChallenge selection(long id, ChallengeCadence cadence, ChallengeDifficulty difficulty) {
        Challenge challenge = new Challenge();
        challenge.setCadence(cadence);
        challenge.setDifficulty(difficulty);

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setId(id);
        selection.setWeekStart(WEEK_START);
        selection.setCadence(cadence);
        selection.setChallenge(challenge);

        return selection;
    }

    private static PlayerChallengeProgress progress(Player player, WeeklyChallenge selection, boolean completed) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(selection);
        progress.setCompleted(completed);

        return progress;
    }
}
