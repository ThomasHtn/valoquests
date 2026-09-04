package io.github.thomashtn.valoquests.campaign.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.henrik.exception.HenrikServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that a background backfill never throws at a caller that is no longer there.
 */
@ExtendWith(MockitoExtension.class)
class AsyncHistoryBackfillRunnerTest {

    @Mock
    private HistoryBackfillService backfillService;

    @InjectMocks
    private AsyncHistoryBackfillRunner runner;

    @Test
    @DisplayName("Runs the walk")
    void shouldRunTheWalk() {
        runner.run();

        verify(backfillService).backfill();
    }

    @Test
    @DisplayName("Logs a failure instead of propagating it into the executor")
    void shouldSwallowAFailure() {
        when(backfillService.backfill()).thenThrow(new HenrikServiceUnavailableException("Henrik is down"));

        runner.run();

        verify(backfillService).backfill();
    }
}
