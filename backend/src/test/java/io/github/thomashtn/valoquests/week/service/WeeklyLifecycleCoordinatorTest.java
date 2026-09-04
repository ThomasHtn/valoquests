package io.github.thomashtn.valoquests.week.service;

import static org.mockito.Mockito.inOrder;

import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
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
 * Verifies the order the rollover opens a week in.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyLifecycleCoordinatorTest {

    /**
     * Monday the week being opened starts on.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 9, 7);

    @Mock
    private WeeklyChallengeSelectionService selectionService;

    @Mock
    private CampaignLifecycleService lifecycleService;

    @Mock
    private CampaignReplayService replayService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-07T00:05:00Z"), ZoneOffset.UTC);

    private WeeklyLifecycleCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new WeeklyLifecycleCoordinator(selectionService, lifecycleService, replayService, clock);
    }

    @Test
    @DisplayName("Settles and closes the week that ended before drawing the one that starts")
    void shouldSettleBeforeDrawing() {
        coordinator.openWeek(WEEK_START);

        InOrder order = inOrder(lifecycleService, replayService, selectionService);
        order.verify(lifecycleService).startIfDue();
        order.verify(replayService).replayRunningCampaign();
        order.verify(lifecycleService).closeIfComplete(clock);
        order.verify(selectionService).selectWeekChallenges(WEEK_START);
        order.verify(selectionService).selectDailyChallenge(WEEK_START);
        order.verifyNoMoreInteractions();
    }
}
