package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
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
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests what a finished week hands the colony: its materials, and the morale its fight moved.
 */
class ColonyMaterialsReaderTest {

    /** Week being priced. */
    private static final LocalDate WEEK = LocalDate.of(2026, 6, 1);

    /** Roster size the fixture run froze on. */
    private static final int ROSTER_SIZE = 7;

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

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE).materials()).isEqualTo(32 + 8);
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

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE).materials()).isEqualTo(847);
    }

    /**
     * Verifies a defeated boss pays per player of the frozen roster, and lifts the morale by what its
     * category is worth.
     */
    @Test
    void shouldPayADefeatedBossPerPlayerOfTheFrozenRoster() {
        givenEncounter(BossCategory.STANDARD, true, true);

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE)).satisfies(outcome -> {
            assertThat(outcome.materials()).isEqualTo(560);
            assertThat(outcome.moraleDelta()).isEqualTo(15.0);
        });
    }

    /**
     * Verifies the three categories are priced apart, in materials and in morale alike.
     */
    @Test
    void shouldPriceTheThreeCategoriesApart() {
        givenEncounter(BossCategory.MINOR, true, true);
        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE).materials()).isEqualTo(420);

        givenEncounter(BossCategory.ELITE, true, true);
        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE)).satisfies(outcome -> {
            assertThat(outcome.materials()).isEqualTo(700);
            assertThat(outcome.moraleDelta()).isEqualTo(20.0);
        });
    }

    /**
     * Verifies a surviving boss brings no materials and costs morale, whatever it was drawn at.
     *
     * <p>That cost is its entire penalty, and it is the only thing in the model that drives morale down.
     */
    @Test
    void shouldCostMoraleAndPayNothingWhenTheBossHolds() {
        givenEncounter(BossCategory.ELITE, true, false);

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE)).satisfies(outcome -> {
            assertThat(outcome.materials()).isZero();
            assertThat(outcome.moraleDelta()).isEqualTo(-20.0);
        });
    }

    /**
     * Verifies a fight still open settles nothing, however it currently stands.
     */
    @Test
    void shouldSettleNothingForAFightThatIsNotFinalized() {
        givenEncounter(BossCategory.STANDARD, false, true);

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE)).satisfies(outcome -> {
            assertThat(outcome.materials()).isZero();
            assertThat(outcome.moraleDelta()).isZero();
        });
    }

    /**
     * Verifies that a week with no fight at all is simply a week with no boss materials and no morale
     * movement either way.
     *
     * <p>What keeps a run ten weeks long instead of ten fights long, and therefore what keeps runs
     * comparable to one another.
     */
    @Test
    void shouldTreatAWeekWithoutAFightAsAWeekWithoutBossMaterials() {
        givenProgress(progress(ChallengeDifficulty.MEDIUM, true));

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE).moraleDelta()).isZero();

        assertThat(reader.outcomeOf(WEEK, ROSTER_SIZE).materials()).isEqualTo(22);
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
     * @param category  category the boss was drawn at
     * @param finalized whether the fight was closed
     * @param defeated  whether the boss went down
     */
    private void givenEncounter(BossCategory category, boolean finalized, boolean defeated) {
        BossCatalogEntry boss = new BossCatalogEntry();
        boss.setCategory(category);

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(WEEK);
        encounter.setBossCatalogEntry(boss);
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
