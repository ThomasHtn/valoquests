package io.github.thomashtn.valoquests.week.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.boss.service.BossChronologyResult;
import io.github.thomashtn.valoquests.boss.service.BossChronologyService;
import io.github.thomashtn.valoquests.boss.service.WeeklyBossSelectionService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the automatic-renewal gate {@link WeeklyLifecycleCoordinator#openWeek(LocalDate)} applies
 * before drawing a new week, and the sweep resolving every past fight left open — the rest of the
 * coordinator is already covered end to end by {@link DefaultWeeklyRolloverServiceTest}.
 */
class WeeklyLifecycleCoordinatorTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 13);

    private static final LocalDate CURRENT_WEEK_START = LocalDate.of(2026, 7, 20);

    private static final Instant ROLLOVER_TIME = Instant.parse("2026-07-20T00:05:00Z");

    private WeeklyChallengeSelectionService weeklyChallengeSelectionService;

    private WeeklyBossSelectionService weeklyBossSelectionService;

    private WeeklyBossEncounterRepository bossEncounterRepository;

    private BossChronologyService bossChronologyService;

    private RunService runService;

    private WeeklyLifecycleCoordinator coordinator;

    @BeforeEach
    void setUp() {
        weeklyChallengeSelectionService = mock(WeeklyChallengeSelectionService.class);
        weeklyBossSelectionService = mock(WeeklyBossSelectionService.class);
        bossEncounterRepository = mock(WeeklyBossEncounterRepository.class);
        bossChronologyService = mock(BossChronologyService.class);
        runService = mock(RunService.class);

        coordinator = new WeeklyLifecycleCoordinator(
            weeklyChallengeSelectionService,
            weeklyBossSelectionService,
            bossEncounterRepository,
            bossChronologyService,
            new DefaultScoringRuleset(),
            runService
        );
    }

    @Test
    @DisplayName("opens nothing when no campaign is running and automatic renewal is off")
    void shouldOpenNothingWithNoCampaignAndAutoRenewOff() {
        when(runService.currentRun()).thenReturn(Optional.empty());
        when(runService.isAutoRenewEnabled()).thenReturn(false);

        coordinator.openWeek(WEEK_START);

        verify(runService, never()).ensureRunFor(any());
        verify(weeklyChallengeSelectionService, never()).selectWeekChallenges(any());
        verify(weeklyBossSelectionService, never()).selectWeekBoss(any());
    }

    @Test
    @DisplayName("still opens the week when no campaign is running but automatic renewal is on")
    void shouldOpenTheWeekWithNoCampaignButAutoRenewOn() {
        when(runService.currentRun()).thenReturn(Optional.empty());
        when(runService.isAutoRenewEnabled()).thenReturn(true);

        coordinator.openWeek(WEEK_START);

        verify(runService).ensureRunFor(WEEK_START);
        verify(weeklyChallengeSelectionService).selectWeekChallenges(WEEK_START);
    }

    @Test
    @DisplayName("keeps running a campaign already under way regardless of automatic renewal")
    void shouldKeepRunningACampaignAlreadyUnderWayRegardlessOfAutoRenew() {
        when(runService.currentRun()).thenReturn(Optional.of(new Run()));
        when(runService.isAutoRenewEnabled()).thenReturn(false);

        coordinator.openWeek(WEEK_START);

        verify(runService).ensureRunFor(WEEK_START);
        verify(weeklyChallengeSelectionService).selectWeekChallenges(WEEK_START);
    }

    @Test
    @DisplayName("records a surviving boss so the campaign map and the colony can read the defeat")
    void shouldRecordASurvivingBoss() {
        WeeklyBossEncounter encounter = openEncounter();

        when(bossChronologyService.computeChronology(eq(WEEK_START), any(), eq(90_000)))
            .thenReturn(BossChronologyResult.survived(60_000));

        coordinator.closePastBossEncounters(CURRENT_WEEK_START, ROLLOVER_TIME);

        assertThat(encounter.isDefeated()).isFalse();
        assertThat(encounter.getDamageDealt()).isEqualTo(60_000);
        assertThat(encounter.getFinalizedAt()).isEqualTo(ROLLOVER_TIME);
        verify(bossEncounterRepository).save(encounter);
    }

    @Test
    @DisplayName("resolves a past fight whose week was finalized without it")
    void shouldResolveAFightLeftOpenByAnAlreadyFinalizedWeek() {
        WeeklyBossEncounter encounter = openEncounter();
        Player finisher = new Player();

        when(bossChronologyService.computeChronology(eq(WEEK_START), any(), eq(90_000)))
            .thenReturn(new BossChronologyResult(true, finisher, null, 95_000));

        coordinator.closePastBossEncounters(CURRENT_WEEK_START, ROLLOVER_TIME);

        assertThat(encounter.isDefeated()).isTrue();
        assertThat(encounter.getDefeatedByPlayer()).isSameAs(finisher);
        assertThat(encounter.getFinalizedAt()).isEqualTo(ROLLOVER_TIME);
    }

    /**
     * Declares one unresolved encounter for {@link #WEEK_START}, as the repository reports it.
     *
     * @return the encounter the sweep will find
     */
    private WeeklyBossEncounter openEncounter() {
        BossCatalogEntry catalogEntry = new BossCatalogEntry();
        catalogEntry.setCode("SENTINEL");
        catalogEntry.setCategory(BossCategory.STANDARD);

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(WEEK_START);
        encounter.setBossCatalogEntry(catalogEntry);
        encounter.setEffectiveHp(90_000);

        when(
            bossEncounterRepository
                .findAllByFinalizedAtIsNullAndWeekStartLessThanOrderByWeekStartAsc(CURRENT_WEEK_START)
        ).thenReturn(List.of(encounter));

        return encounter;
    }
}
