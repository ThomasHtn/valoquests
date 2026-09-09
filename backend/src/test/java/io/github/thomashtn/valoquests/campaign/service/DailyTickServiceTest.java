package io.github.thomashtn.valoquests.campaign.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the order the tick closes a day in.
 */
@ExtendWith(MockitoExtension.class)
class DailyTickServiceTest {

    /**
     * Instant the tick fires at.
     */
    private static final Instant TICK_TIME = Instant.parse("2026-09-07T00:10:00Z");

    /**
     * Day the tick opens.
     */
    private static final LocalDate TICK_DAY = LocalDate.of(2026, 9, 7);

    @Mock
    private WeeklyChallengeSelectionService selectionService;

    @Mock
    private ChallengeRecalculationService recalculationService;

    @Mock
    private CampaignLifecycleService lifecycleService;

    @Mock
    private CampaignReplayService replayService;

    private DailyTickService dailyTickService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TICK_TIME, ZoneOffset.UTC);
        dailyTickService = new DailyTickService(
            selectionService,
            recalculationService,
            lifecycleService,
            replayService,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Draws the day and starts a due campaign before replaying, and never closes one")
    void shouldDrawThenReplayWithoutClosing() {
        dailyTickService.run();

        InOrder order = inOrder(selectionService, recalculationService, lifecycleService, replayService);
        order.verify(selectionService).selectDailyChallenge(TICK_DAY);
        order.verify(recalculationService).recalculateCurrentWeekProgress();
        order.verify(lifecycleService).startIfDue();
        order.verify(replayService).replayRunningCampaign();
        verify(lifecycleService, never()).closeIfComplete(any());
    }
}
