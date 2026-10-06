package io.github.thomashtn.valoquests.week.service;

import static org.mockito.Mockito.inOrder;

import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.DailyChallengeDrawService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeDrawService;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import java.time.LocalDate;
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
class WeekOpenerTest {

    /**
     * Monday the week being opened starts on.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 9, 7);

    @Mock
    private CampaignLifecycleService lifecycleService;

    @Mock
    private CampaignReplayService replayService;

    @Mock
    private WeeklyChallengeDrawService weeklyDrawService;

    @Mock
    private DailyChallengeDrawService dailyDrawService;

    @Mock
    private ChallengeRecalculationService challengeRecalculationService;

    @Mock
    private RankingRecalculationService rankingRecalculationService;

    private WeekOpener opener;

    @BeforeEach
    void setUp() {
        opener = new WeekOpener(
            lifecycleService,
            replayService,
            weeklyDrawService,
            dailyDrawService,
            challengeRecalculationService,
            rankingRecalculationService
        );
    }

    @Test
    @DisplayName("Settles and closes the week that ended before drawing the one that starts, then ranks it at zero")
    void shouldSettleBeforeDrawing() {
        opener.openWeek(WEEK_START);

        InOrder order = inOrder(
            lifecycleService,
            replayService,
            weeklyDrawService,
            dailyDrawService,
            challengeRecalculationService,
            rankingRecalculationService
        );
        order.verify(lifecycleService).startIfDue();
        order.verify(replayService).replayRunningCampaign();
        order.verify(lifecycleService).closeIfComplete();
        order.verify(weeklyDrawService).selectWeekChallenges(WEEK_START);
        order.verify(dailyDrawService).selectDailyChallenge(WEEK_START);
        order.verify(challengeRecalculationService).recalculateWeekProgress(WEEK_START);
        order.verify(rankingRecalculationService).recalculateWeek(WEEK_START);
        order.verifyNoMoreInteractions();
    }
}
