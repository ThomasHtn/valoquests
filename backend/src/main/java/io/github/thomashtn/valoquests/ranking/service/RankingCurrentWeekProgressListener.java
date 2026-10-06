package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.challenge.service.CurrentWeekProgressListener;
import org.springframework.stereotype.Component;

/**
 * Rebuilds the current ranking each time the current week's challenge progress is rebuilt.
 */
@Component
public class RankingCurrentWeekProgressListener implements CurrentWeekProgressListener {

    /**
     * Service rebuilding the current weekly ranking.
     */
    private final RankingRecalculationService rankingRecalculationService;

    /**
     * Creates the listener.
     *
     * @param rankingRecalculationService ranking recalculation service
     */
    public RankingCurrentWeekProgressListener(RankingRecalculationService rankingRecalculationService) {
        this.rankingRecalculationService = rankingRecalculationService;
    }

    /**
     * Rebuilds the current ranking from the progress just written.
     */
    @Override
    public void currentWeekProgressRecalculated() {
        rankingRecalculationService.recalculateCurrentRanking();
    }
}
