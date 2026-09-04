package io.github.thomashtn.valoquests.campaign.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
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
 * Verifies the order the midnight tick closes a day in, and that a failure never escapes it.
 */
@ExtendWith(MockitoExtension.class)
class CampaignDailyTickSchedulerTest {

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
    private CampaignLifecycleService lifecycleService;

    @Mock
    private CampaignReplayService replayService;

    private Clock clock;

    private CampaignDailyTickScheduler scheduler;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(TICK_TIME, ZoneOffset.UTC);
        scheduler = new CampaignDailyTickScheduler(
            selectionService,
            lifecycleService,
            replayService,
            new WeekCalendar(clock, ZoneOffset.UTC),
            clock
        );
    }

    @Test
    @DisplayName("Replays the campaign before closing it, so its last Sunday is settled")
    void shouldReplayBeforeClosing() {
        scheduler.tick();

        InOrder order = inOrder(selectionService, lifecycleService, replayService);
        order.verify(selectionService).selectDailyChallenge(TICK_DAY);
        order.verify(lifecycleService).startIfDue();
        order.verify(replayService).replayRunningCampaign();
        order.verify(lifecycleService).closeIfComplete(clock);
    }

    @Test
    @DisplayName("Swallows a failure so the scheduler keeps firing the next night")
    void shouldSwallowAFailure() {
        doThrow(new IllegalStateException("the daily pool is empty"))
            .when(selectionService).selectDailyChallenge(TICK_DAY);

        scheduler.tick();

        verify(selectionService).selectDailyChallenge(TICK_DAY);
        verifyNoInteractions(lifecycleService, replayService);
    }
}
