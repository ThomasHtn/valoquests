package io.github.thomashtn.valoquests.boss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a fight is sized from what the roster actually produces, and that no single week can
 * move that measurement far enough to break the campaign.
 */
class BossCalibrationServiceTest {

    /** Barèmes owning the seed, window and band. */
    private static final ScoringRuleset RULESET = new DefaultScoringRuleset();

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Service under test. */
    private BossCalibrationService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        encounterRepository = mock(WeeklyBossEncounterRepository.class);
        service = new BossCalibrationService(encounterRepository, RULESET);
    }

    /**
     * Verifies that a campaign with no closed week opens on the seed.
     */
    @Test
    void shouldFallBackOnTheSeedWhenNoWeekIsClosed() {
        when(encounterRepository.findRecentFinalized(any())).thenReturn(List.of());

        assertThat(service.referenceDamagePerPlayer())
            .isEqualTo(RULESET.seedReferenceDamagePerPlayer());
    }

    /**
     * Verifies that one closed week is not enough to move the bar.
     */
    @Test
    void shouldFallBackOnTheSeedWithASingleClosedWeek() {
        when(encounterRepository.findRecentFinalized(any()))
            .thenReturn(List.of(encounter(70_000, 7)));

        assertThat(service.referenceDamagePerPlayer())
            .isEqualTo(RULESET.seedReferenceDamagePerPlayer());
    }

    /**
     * Verifies that the reference follows the roster's real per-player output.
     */
    @Test
    void shouldMeasureThePerPlayerOutputOfClosedWeeks() {
        // 6 000, 8 000, 10 000 and 12 000 per player: the median of the middle pair is 9 000.
        when(encounterRepository.findRecentFinalized(any())).thenReturn(List.of(
            encounter(36_000, 6),
            encounter(48_000, 6),
            encounter(60_000, 6),
            encounter(72_000, 6)
        ));

        assertThat(service.referenceDamagePerPlayer()).isEqualTo(9_000);
    }

    /**
     * Verifies that the roster size, not just the raw total, is what the measurement divides by.
     */
    @Test
    void shouldNormaliseByTheRosterThatFacedEachFight() {
        // Same 60 000 total, but three players rather than six: twice the per-player output.
        when(encounterRepository.findRecentFinalized(any())).thenReturn(List.of(
            encounter(60_000, 3),
            encounter(60_000, 3),
            encounter(60_000, 3)
        ));

        assertThat(service.referenceDamagePerPlayer()).isEqualTo(20_000);
    }

    /**
     * The property that keeps one exceptional week from resizing the campaign: the median ignores it,
     * where a mean would carry it into every following fight.
     */
    @Test
    void shouldIgnoreASingleMarathonWeek() {
        List<WeeklyBossEncounter> ordinaryWeeks = List.of(
            encounter(42_000, 6),
            encounter(42_000, 6),
            encounter(42_000, 6)
        );

        when(encounterRepository.findRecentFinalized(any())).thenReturn(ordinaryWeeks);
        int withoutMarathon = service.referenceDamagePerPlayer();

        when(encounterRepository.findRecentFinalized(any())).thenReturn(List.of(
            ordinaryWeeks.get(0),
            ordinaryWeeks.get(1),
            ordinaryWeeks.get(2),
            encounter(600_000, 6)
        ));

        assertThat(service.referenceDamagePerPlayer()).isEqualTo(withoutMarathon);
    }

    /**
     * Verifies that a roster that stops playing cannot drive the next fight to nothing.
     */
    @Test
    void shouldNotLetAQuietStretchCollapseTheReference() {
        when(encounterRepository.findRecentFinalized(any())).thenReturn(List.of(
            encounter(0, 6),
            encounter(0, 6),
            encounter(600, 6)
        ));

        int floor = RULESET.seedReferenceDamagePerPlayer()
            * RULESET.calibrationFloorPercent() / 100;

        assertThat(service.referenceDamagePerPlayer()).isEqualTo(floor);
    }

    /**
     * Verifies the symmetric guard: a sustained surge cannot make the fight unreachable either.
     */
    @Test
    void shouldNotLetASustainedSurgeRunAway() {
        when(encounterRepository.findRecentFinalized(any())).thenReturn(List.of(
            encounter(6_000_000, 6),
            encounter(6_000_000, 6),
            encounter(6_000_000, 6)
        ));

        int ceiling = RULESET.seedReferenceDamagePerPlayer()
            * RULESET.calibrationCeilingPercent() / 100;

        assertThat(service.referenceDamagePerPlayer()).isEqualTo(ceiling);
    }

    /** Builds a finalized encounter that dealt a known total against a known roster. */
    private WeeklyBossEncounter encounter(int damageDealt, int activePlayerCount) {
        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setDamageDealt(damageDealt);
        encounter.setActivePlayerCount(activePlayerCount);
        return encounter;
    }
}
