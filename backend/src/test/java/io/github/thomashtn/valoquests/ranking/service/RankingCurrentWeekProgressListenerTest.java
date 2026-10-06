package io.github.thomashtn.valoquests.ranking.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that rebuilt challenge progress is followed by the current ranking.
 */
class RankingCurrentWeekProgressListenerTest {

    /**
     * Rebuilds only the current ranking, never a past week's.
     */
    @Test
    @DisplayName("Rebuilds the current ranking once the current week's progress is rebuilt")
    void rebuildsCurrentRanking() {
        RankingRecalculationService rankingRecalculationService = mock(RankingRecalculationService.class);

        new RankingCurrentWeekProgressListener(rankingRecalculationService).currentWeekProgressRecalculated();

        verify(rankingRecalculationService).recalculateCurrentRanking();
        verifyNoMoreInteractions(rankingRecalculationService);
    }
}
