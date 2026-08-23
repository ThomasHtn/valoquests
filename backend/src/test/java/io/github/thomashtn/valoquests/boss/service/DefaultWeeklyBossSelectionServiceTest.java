package io.github.thomashtn.valoquests.boss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRulesetRegistry;
import io.github.thomashtn.valoquests.scoring.ScoringRulesetV1;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests deterministic, non-repeating weekly boss selection and difficulty-modifier derivation.
 */
class DefaultWeeklyBossSelectionServiceTest {

    /** Week resolved from the fixed application clock. */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /** Catalogue repository dependency. */
    private BossCatalogEntryRepository catalogRepository;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Service under test. */
    private DefaultWeeklyBossSelectionService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        catalogRepository = mock(BossCatalogEntryRepository.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);

        when(encounterRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Clock clock = Clock.fixed(Instant.parse("2026-07-21T10:00:00Z"), ZoneOffset.UTC);
        ScoringRulesetRegistry rulesetRegistry =
            new ScoringRulesetRegistry(List.of(new ScoringRulesetV1()));

        service = new DefaultWeeklyBossSelectionService(
            catalogRepository,
            encounterRepository,
            rulesetRegistry,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that an existing selection is returned as-is, never replaced.
     */
    @Test
    void shouldNeverReplaceAnExistingSelection() {
        WeeklyBossEncounter existing = new WeeklyBossEncounter();
        existing.setWeekStart(WEEK_START);

        when(encounterRepository.findByWeekStart(WEEK_START))
            .thenReturn(Optional.of(existing));

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result).isSameAs(existing);
        verify(encounterRepository, never()).save(any());
    }

    /**
     * Verifies that the boss cannot repeat before the whole catalogue has been drawn once.
     */
    @Test
    void shouldNotRepeatABossUntilTheCatalogueHasFullyCycled() {
        BossCatalogEntry bossA = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        BossCatalogEntry bossB = createBoss(2L, "BOSS_B", BossCategory.STANDARD);
        BossCatalogEntry bossC = createBoss(3L, "BOSS_C", BossCategory.STANDARD);

        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc())
            .thenReturn(List.of(bossA, bossB, bossC));

        // Two of the three bosses were already drawn in previous weeks, still within one cycle.
        when(encounterRepository.findAllByOrderByWeekStartAsc()).thenReturn(List.of(
            createEncounter(WEEK_START.minusWeeks(2), bossA),
            createEncounter(WEEK_START.minusWeeks(1), bossB)
        ));

        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.empty());
        when(encounterRepository.findLatestFinalized()).thenReturn(Optional.empty());

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        // Only bossC has not been drawn yet in the current cycle, so it is the only valid candidate.
        assertThat(result.getBossCatalogEntry()).isSameAs(bossC);
    }

    /**
     * Verifies that a completed cycle resets and lets any boss be drawn again.
     */
    @Test
    void shouldAllowRepetitionOnceTheCatalogueCycleCompletes() {
        BossCatalogEntry bossA = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        BossCatalogEntry bossB = createBoss(2L, "BOSS_B", BossCategory.STANDARD);

        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc())
            .thenReturn(List.of(bossA, bossB));

        // Both bosses of this two-entry catalogue were already drawn: the cycle is complete, so the
        // next draw must be free to pick from the full catalogue again.
        when(encounterRepository.findAllByOrderByWeekStartAsc()).thenReturn(List.of(
            createEncounter(WEEK_START.minusWeeks(2), bossA),
            createEncounter(WEEK_START.minusWeeks(1), bossB)
        ));

        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.empty());
        when(encounterRepository.findLatestFinalized()).thenReturn(Optional.empty());

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getBossCatalogEntry()).isIn(bossA, bossB);
    }

    /**
     * Verifies that the difficulty modifier increases after a victory, bounded at 130%.
     */
    @Test
    void shouldIncreaseModifierAfterVictoryAndClampAtUpperBound() {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(boss));
        when(encounterRepository.findAllByOrderByWeekStartAsc()).thenReturn(List.of());
        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.empty());

        WeeklyBossEncounter previous = createEncounter(WEEK_START.minusWeeks(1), boss);
        previous.setDifficultyModifierPercent(128);
        previous.setDefeated(true);
        when(encounterRepository.findLatestFinalized()).thenReturn(Optional.of(previous));

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getDifficultyModifierPercent()).isEqualTo(130);
        assertThat(result.getEffectiveHp())
            .isEqualTo((int) Math.round(new ScoringRulesetV1().bossBaseHp(BossCategory.STANDARD) * 1.30));
    }

    /**
     * Verifies that the difficulty modifier decreases after a survival, bounded at 70%.
     */
    @Test
    void shouldDecreaseModifierAfterSurvivalAndClampAtLowerBound() {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", BossCategory.STANDARD);
        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(boss));
        when(encounterRepository.findAllByOrderByWeekStartAsc()).thenReturn(List.of());
        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.empty());

        WeeklyBossEncounter previous = createEncounter(WEEK_START.minusWeeks(1), boss);
        previous.setDifficultyModifierPercent(75);
        previous.setDefeated(false);
        when(encounterRepository.findLatestFinalized()).thenReturn(Optional.of(previous));

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        assertThat(result.getDifficultyModifierPercent()).isEqualTo(70);
    }

    /**
     * Verifies the neutral 100% modifier used for the very first boss week.
     */
    @Test
    void shouldStartAtTheNeutralModifierWhenNoWeekWasEverFinalized() {
        BossCatalogEntry boss = createBoss(1L, "BOSS_A", BossCategory.MINOR);
        when(catalogRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(boss));
        when(encounterRepository.findAllByOrderByWeekStartAsc()).thenReturn(List.of());
        when(encounterRepository.findByWeekStart(WEEK_START)).thenReturn(Optional.empty());
        when(encounterRepository.findLatestFinalized()).thenReturn(Optional.empty());

        WeeklyBossEncounter result = service.selectWeekBoss(WEEK_START);

        ScoringRuleset ruleset = new ScoringRulesetV1();
        assertThat(result.getDifficultyModifierPercent()).isEqualTo(100);
        assertThat(result.getBaseHp()).isEqualTo(ruleset.bossBaseHp(BossCategory.MINOR));
        assertThat(result.getEffectiveHp()).isEqualTo(ruleset.bossBaseHp(BossCategory.MINOR));
        assertThat(result.getRulesetVersion()).isEqualTo(1);

        ArgumentCaptor<WeeklyBossEncounter> captor = ArgumentCaptor.forClass(WeeklyBossEncounter.class);
        verify(encounterRepository).save(captor.capture());
        assertThat(captor.getValue().getWeekStart()).isEqualTo(WEEK_START);
    }

    /** Creates a catalogue boss fixture. */
    private BossCatalogEntry createBoss(Long id, String code, BossCategory category) {
        BossCatalogEntry boss = new BossCatalogEntry();
        boss.setId(id);
        boss.setCode(code);
        boss.setName(code);
        boss.setDescription("Fixture boss " + code);
        boss.setCategory(category);
        boss.setEnabled(true);
        return boss;
    }

    /** Creates a past encounter fixture referencing a drawn boss. */
    private WeeklyBossEncounter createEncounter(LocalDate weekStart, BossCatalogEntry boss) {
        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(weekStart);
        encounter.setBossCatalogEntry(boss);
        return encounter;
    }
}
