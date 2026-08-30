package io.github.thomashtn.valoquests.week.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.boss.service.BossChronologyService;
import io.github.thomashtn.valoquests.boss.service.WeeklyBossSelectionService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the automatic-renewal gate {@link WeeklyLifecycleCoordinator#openWeek(LocalDate)} applies
 * before drawing a new week — the rest of that method is already covered end to end by
 * {@link DefaultWeeklyRolloverServiceTest}.
 */
class WeeklyLifecycleCoordinatorTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 13);

    private WeeklyChallengeSelectionService weeklyChallengeSelectionService;

    private WeeklyBossSelectionService weeklyBossSelectionService;

    private RunService runService;

    private WeeklyLifecycleCoordinator coordinator;

    @BeforeEach
    void setUp() {
        weeklyChallengeSelectionService = mock(WeeklyChallengeSelectionService.class);
        weeklyBossSelectionService = mock(WeeklyBossSelectionService.class);
        runService = mock(RunService.class);

        coordinator = new WeeklyLifecycleCoordinator(
            weeklyChallengeSelectionService,
            weeklyBossSelectionService,
            mock(WeeklyBossEncounterRepository.class),
            mock(BossChronologyService.class),
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
}
