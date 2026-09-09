package io.github.thomashtn.valoquests.campaign.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.github.thomashtn.valoquests.campaign.service.DailyTickService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the midnight tick delegates, and that a failure never escapes it.
 */
@ExtendWith(MockitoExtension.class)
class CampaignDailyTickSchedulerTest {

    @Mock
    private DailyTickService dailyTickService;

    @InjectMocks
    private CampaignDailyTickScheduler scheduler;

    @Test
    @DisplayName("Runs the tick the administrative route runs")
    void shouldRunTheTick() {
        scheduler.tick();

        verify(dailyTickService).run();
    }

    @Test
    @DisplayName("Swallows a failure so the scheduler keeps firing the next night")
    void shouldSwallowAFailure() {
        doThrow(new IllegalStateException("the daily pool is empty")).when(dailyTickService).run();

        scheduler.tick();

        verify(dailyTickService).run();
    }
}
