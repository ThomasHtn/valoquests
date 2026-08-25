package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests what a finished week is worth in materials.
 */
class ColonyMaterialsReaderTest {

    /** Week being priced. */
    private static final LocalDate WEEK = LocalDate.of(2026, 6, 1);

    /** Progress repository dependency. */
    private PlayerChallengeProgressRepository progressRepository;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Calibration the materials are priced with. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /** Reader under test. */
    private ColonyMaterialsReader reader;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        progressRepository = mock(PlayerChallengeProgressRepository.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);

        lenient().when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(any()))
            .thenReturn(List.of());
        lenient().when(encounterRepository.findByWeekStart(any())).thenReturn(Optional.empty());

        reader = new ColonyMaterialsReader(progressRepository, encounterRepository, ruleset);
    }

    /**
     * Verifies that only completed challenges pay, and that each one pays its difficulty's rate.
     */
    @Test
    void shouldPayOnlyForCompletedChallenges() {
        givenProgress(
            progress(ChallengeDifficulty.HARD, true),
            progress(ChallengeDifficulty.EASY, true),
            progress(ChallengeDifficulty.VERY_HARD, false)
        );

        assertThat(reader.materialsOf(WEEK)).isEqualTo(32 + 8);
    }

    /**
     * Verifies that a perfect week, seven players over five challenges, is worth 847 materials.
     */
    @Test
    void shouldPriceAPerfectWeekAtEightHundredAndFortySeven() {
        List<PlayerChallengeProgress> perfectWeek = List.of(
            ChallengeDifficulty.EASY,
            ChallengeDifficulty.NORMAL,
            ChallengeDifficulty.MEDIUM,
            ChallengeDifficulty.HARD,
            ChallengeDifficulty.VERY_HARD
        ).stream()
            .flatMap(difficulty -> java.util.stream.IntStream.range(0, 7)
                .mapToObj(ignored -> progress(difficulty, true)))
            .toList();

        givenProgress(perfectWeek.toArray(new PlayerChallengeProgress[0]));

        assertThat(reader.materialsOf(WEEK)).isEqualTo(847);
    }

    /**
     * Verifies that a defeated boss adds four hundred materials.
     */
    @Test
    void shouldPayFourHundredForADefeatedBoss() {
        givenEncounter(true, true);

        assertThat(reader.materialsOf(WEEK)).isEqualTo(400);
    }

    /**
     * Verifies that a surviving boss brings nothing, which is its entire cost.
     */
    @Test
    void shouldPayNothingForASurvivingBoss() {
        givenEncounter(true, false);

        assertThat(reader.materialsOf(WEEK)).isZero();
    }

    /**
     * Verifies that a fight still open pays nothing, however it currently stands.
     */
    @Test
    void shouldPayNothingForAFightThatIsNotFinalized() {
        givenEncounter(false, true);

        assertThat(reader.materialsOf(WEEK)).isZero();
    }

    /**
     * Verifies that a week with no fight at all is simply a week with no boss materials.
     *
     * <p>What keeps a run ten weeks long instead of ten fights long, and therefore what keeps runs
     * comparable to one another.
     */
    @Test
    void shouldTreatAWeekWithoutAFightAsAWeekWithoutBossMaterials() {
        givenProgress(progress(ChallengeDifficulty.MEDIUM, true));

        assertThat(reader.materialsOf(WEEK)).isEqualTo(22);
    }

    /**
     * Registers the week's progress rows.
     *
     * @param rows progress rows
     */
    private void givenProgress(PlayerChallengeProgress... rows) {
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK))
            .thenReturn(List.of(rows));
    }

    /**
     * Registers the week's fight.
     *
     * @param finalized whether the fight was closed
     * @param defeated  whether the boss went down
     */
    private void givenEncounter(boolean finalized, boolean defeated) {
        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(WEEK);
        encounter.setDefeated(defeated);
        encounter.setFinalizedAt(finalized ? Instant.parse("2026-06-08T00:05:00Z") : null);

        when(encounterRepository.findByWeekStart(WEEK)).thenReturn(Optional.of(encounter));
    }

    /**
     * Builds one player's progress on one challenge.
     *
     * @param difficulty challenge difficulty
     * @param completed  whether the player completed it
     * @return progress row
     */
    private static PlayerChallengeProgress progress(
        ChallengeDifficulty difficulty,
        boolean completed
    ) {
        Challenge challenge = new Challenge();
        challenge.setDifficulty(difficulty);

        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(WEEK);
        weeklyChallenge.setChallenge(challenge);

        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCompleted(completed);

        return progress;
    }
}
