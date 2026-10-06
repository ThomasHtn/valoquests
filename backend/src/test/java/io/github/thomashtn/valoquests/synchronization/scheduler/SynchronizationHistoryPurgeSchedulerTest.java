package io.github.thomashtn.valoquests.synchronization.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.synchronization.service.SynchronizationHistoryPurger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SynchronizationHistoryPurgeScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class SynchronizationHistoryPurgeSchedulerTest {

    @Mock
    private SynchronizationHistoryPurger historyPurger;

    /**
     * Verifies that the scheduled run delegates to the purger.
     */
    @Test
    @DisplayName("Purges the quiet executions on schedule")
    void shouldPurgeQuietExecutions() {
        new SynchronizationHistoryPurgeScheduler(historyPurger).purge();

        verify(historyPurger).purgeQuietExecutions();
    }

    /**
     * Verifies that a failure does not escape the scheduled method.
     */
    @Test
    @DisplayName("Keeps the schedule alive when the purge fails")
    void shouldKeepTheScheduleAliveWhenThePurgeFails() {
        when(historyPurger.purgeQuietExecutions()).thenThrow(new IllegalStateException("Database down"));

        assertThatCode(new SynchronizationHistoryPurgeScheduler(historyPurger)::purge)
            .doesNotThrowAnyException();
    }
}
