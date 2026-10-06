package io.github.thomashtn.valoquests.campaign.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.thomashtn.valoquests.campaign.service.DailyTickService;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the midnight tick delegates under the history lock, and that a failure never escapes it.
 */
@ExtendWith(MockitoExtension.class)
class CampaignDailyTickSchedulerTest {

    @Mock
    private DailyTickService dailyTickService;

    /**
     * Lock shared with the other jobs writing the match history.
     */
    private MatchHistoryLock lock;

    /**
     * Scheduler under test.
     */
    private CampaignDailyTickScheduler scheduler;

    /**
     * Builds the scheduler over a free lock.
     */
    @BeforeEach
    void setUp() {
        lock = new MatchHistoryLock();
        scheduler = new CampaignDailyTickScheduler(dailyTickService, lock);
    }

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
        assertThat(lock.runIfFree(() -> { })).isTrue();
    }

    @Test
    @DisplayName("Waits for another guarded job to finish, then runs the tick")
    void shouldWaitForAnotherGuardedJob() throws InterruptedException {
        lock.acquireOrReject();
        Thread runningJob = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            lock.release();
        });
        runningJob.start();

        scheduler.tick();
        runningJob.join();

        verify(dailyTickService).run();
    }

    @Test
    @DisplayName("Gives up without running the tick and keeps the interrupt flag when interrupted")
    void shouldGiveUpWhenInterruptedWhileWaiting() {
        lock.acquireOrReject();

        Thread.currentThread().interrupt();
        try {
            scheduler.tick();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        verifyNoInteractions(dailyTickService);
    }
}
