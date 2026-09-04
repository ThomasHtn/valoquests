package io.github.thomashtn.valoquests.week.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Tests atomic and idempotent weekly rollover orchestration.
 */
class DefaultWeeklyRolloverServiceTest {

    /**
     * Previous week resolved from the fixed clock.
     */
    private static final LocalDate PREVIOUS_WEEK_START =
        LocalDate.of(2026, 7, 13);

    /**
     * Current week resolved from the fixed clock.
     */
    private static final LocalDate CURRENT_WEEK_START =
        LocalDate.of(2026, 7, 20);

    /**
     * Older week left open by a rollover that never ran.
     */
    private static final LocalDate MISSED_WEEK_START =
        LocalDate.of(2026, 6, 29);

    /**
     * Fixed rollover timestamp.
     */
    private static final Instant ROLLOVER_TIME =
        Instant.parse("2026-07-20T00:05:00Z");

    /**
     * Weekly challenge repository dependency.
     */
    private WeeklyChallengeRepository
        weeklyChallengeRepository;

    /**
     * Weekly score repository dependency.
     */
    private WeeklyPlayerScoreRepository
        weeklyPlayerScoreRepository;

    /**
     * Ranking recalculation dependency.
     */
    private RankingRecalculationService
        rankingRecalculationService;

    /**
     * Weekly lifecycle coordination dependency.
     */
    private WeeklyLifecycleCoordinator
        weeklyLifecycleCoordinator;

    /**
     * Challenge progress recalculation dependency.
     */
    private ChallengeRecalculationService
        challengeRecalculationService;

    /**
     * Service under test.
     */
    private DefaultWeeklyRolloverService service;

    /**
     * Creates mocked dependencies before each test.
     */
    @BeforeEach
    void setUp() {
        weeklyChallengeRepository =
            mock(WeeklyChallengeRepository.class);

        weeklyPlayerScoreRepository =
            mock(WeeklyPlayerScoreRepository.class);

        rankingRecalculationService =
            mock(RankingRecalculationService.class);

        weeklyLifecycleCoordinator =
            mock(WeeklyLifecycleCoordinator.class);

        challengeRecalculationService =
            mock(ChallengeRecalculationService.class);

        Clock clock = Clock.fixed(
            ROLLOVER_TIME,
            ZoneOffset.UTC
        );

        service = new DefaultWeeklyRolloverService(
            weeklyChallengeRepository,
            weeklyPlayerScoreRepository,
            rankingRecalculationService,
            weeklyLifecycleCoordinator,
            challengeRecalculationService,
            clock,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that the previous ranking and challenges are finalized before
     * the new challenge pack is prepared.
     */
    @Test
    void shouldFinalizePreviousWeekAndPrepareCurrentWeek() {
        WeeklyChallenge firstChallenge =
            new WeeklyChallenge();

        WeeklyChallenge secondChallenge =
            new WeeklyChallenge();

        WeeklyPlayerScore firstScore =
            new WeeklyPlayerScore();

        WeeklyPlayerScore secondScore =
            new WeeklyPlayerScore();

        givenPendingWeeks(PREVIOUS_WEEK_START);

        when(
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(
                    PREVIOUS_WEEK_START
                )
        ).thenReturn(
            List.of(
                firstChallenge,
                secondChallenge
            )
        );

        when(
            weeklyPlayerScoreRepository
                .findAllByWeekStartOrderByPositionAsc(
                    PREVIOUS_WEEK_START
                )
        ).thenReturn(
            List.of(
                firstScore,
                secondScore
            )
        );

        service.rolloverIfNeeded();

        // Progress before ranking: the ranking is derived from the progress, so rebuilding it
        // first would freeze the week on values that ignore the matches just imported.
        InOrder rebuildOrder = inOrder(
            challengeRecalculationService,
            rankingRecalculationService
        );

        rebuildOrder.verify(challengeRecalculationService)
            .recalculateWeekProgress(PREVIOUS_WEEK_START);

        rebuildOrder.verify(rankingRecalculationService)
            .recalculateWeek(PREVIOUS_WEEK_START);

        verify(weeklyChallengeRepository)
            .saveAll(
                List.of(
                    firstChallenge,
                    secondChallenge
                )
            );

        verify(weeklyPlayerScoreRepository)
            .saveAll(
                List.of(
                    firstScore,
                    secondScore
                )
            );

        verify(weeklyLifecycleCoordinator)
            .openWeek(
                CURRENT_WEEK_START
            );


        assertThat(firstChallenge.getFinalizedAt())
            .isEqualTo(ROLLOVER_TIME);

        assertThat(secondChallenge.getFinalizedAt())
            .isEqualTo(ROLLOVER_TIME);

        assertThat(firstScore.getFinalizedAt())
            .isEqualTo(ROLLOVER_TIME);

        assertThat(secondScore.getFinalizedAt())
            .isEqualTo(ROLLOVER_TIME);
    }

    /**
     * Verifies that every week left open by a missed rollover is caught up, oldest first.
     */
    @Test
    void shouldCatchUpEveryPendingWeek() {
        WeeklyChallenge missedWeekChallenge =
            new WeeklyChallenge();

        WeeklyChallenge previousWeekChallenge =
            new WeeklyChallenge();

        givenPendingWeeks(
            MISSED_WEEK_START,
            PREVIOUS_WEEK_START
        );

        when(
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(
                    MISSED_WEEK_START
                )
        ).thenReturn(
            List.of(missedWeekChallenge)
        );

        when(
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(
                    PREVIOUS_WEEK_START
                )
        ).thenReturn(
            List.of(previousWeekChallenge)
        );

        service.rolloverIfNeeded();

        InOrder catchUpOrder = inOrder(
            rankingRecalculationService,
            weeklyLifecycleCoordinator
        );

        catchUpOrder.verify(rankingRecalculationService)
            .recalculateWeek(MISSED_WEEK_START);

        catchUpOrder.verify(rankingRecalculationService)
            .recalculateWeek(PREVIOUS_WEEK_START);

        // The new week is opened once every caught-up week's ranking has been rebuilt: opening it
        // settles the campaign week that just ended, against the rankings those passes just froze.
        catchUpOrder.verify(weeklyLifecycleCoordinator)
            .openWeek(CURRENT_WEEK_START);

        verify(challengeRecalculationService)
            .recalculateWeekProgress(MISSED_WEEK_START);

        assertThat(missedWeekChallenge.getFinalizedAt())
            .isEqualTo(ROLLOVER_TIME);

        assertThat(previousWeekChallenge.getFinalizedAt())
            .isEqualTo(ROLLOVER_TIME);
    }

    /**
     * Verifies that no week is re-finalized when none is still open, while the past fights are still
     * swept.
     *
     * <p>Covers both an already finalized previous week and the very first application week: in
     * either case the week is not pending.</p>
     *
     * <p>The sweep runs regardless, which is the whole point of driving it off the encounters. It used
     * to ride on the pack query, so a week whose challenges were finalized without its fight — a
     * rollover interrupted between the two, a boss drawn after the pack had closed — kept an unresolved
     * encounter forever: the campaign map left it locked as an upcoming week, and the colony, which
     * reads the same rows, never charged the morale a surviving boss costs.</p>
     */
    @Test
    void shouldSweepPastFightsWhenNoWeekIsPending() {
        givenPendingWeeks();

        service.rolloverIfNeeded();

        verify(
            rankingRecalculationService,
            never()
        ).recalculateWeek(PREVIOUS_WEEK_START);

        verify(
            weeklyPlayerScoreRepository,
            never()
        ).saveAll(
            org.mockito.ArgumentMatchers.anyList()
        );

        verify(weeklyLifecycleCoordinator)
            .openWeek(
                CURRENT_WEEK_START
            );

    }

    /**
     * Verifies that the newly opened week gets its progress and ranking rows straight away, after
     * its pack and boss have been drawn.
     *
     * <p>Without them the current ranking stays empty until the next synchronization, and every
     * screen reading it shows its empty state instead of a week sitting at zero.
     */
    @Test
    void shouldOpenCurrentWeekRankingAtZero() {
        givenPendingWeeks();

        service.rolloverIfNeeded();

        InOrder openOrder = inOrder(
            weeklyLifecycleCoordinator,
            challengeRecalculationService,
            rankingRecalculationService
        );

        openOrder.verify(weeklyLifecycleCoordinator)
            .openWeek(CURRENT_WEEK_START);

        openOrder.verify(challengeRecalculationService)
            .recalculateWeekProgress(CURRENT_WEEK_START);

        openOrder.verify(rankingRecalculationService)
            .recalculateWeek(CURRENT_WEEK_START);
    }

    /**
     * Verifies that a partially finalized pack is rejected instead of being
     * silently repaired.
     */
    @Test
    void shouldRejectPartiallyFinalizedPreviousWeek() {
        WeeklyChallenge finalizedChallenge =
            new WeeklyChallenge();

        finalizedChallenge.setFinalizedAt(
            ROLLOVER_TIME
        );

        WeeklyChallenge activeChallenge =
            new WeeklyChallenge();

        givenPendingWeeks(PREVIOUS_WEEK_START);

        when(
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(
                    PREVIOUS_WEEK_START
                )
        ).thenReturn(
            List.of(
                finalizedChallenge,
                activeChallenge
            )
        );

        assertThatThrownBy(
            service::rolloverIfNeeded
        )
            .isInstanceOf(
                IllegalStateException.class
            )
            .hasMessageContaining(
                PREVIOUS_WEEK_START.toString()
            );

        verify(
            rankingRecalculationService,
            never()
        ).recalculateWeek(
            PREVIOUS_WEEK_START
        );

        verify(
            weeklyLifecycleCoordinator,
            never()
        ).openWeek(
            CURRENT_WEEK_START
        );
    }

    /**
     * Declares the past weeks the repository reports as still open.
     *
     * @param weekStarts pending week identifiers, oldest first
     */
    private void givenPendingWeeks(LocalDate... weekStarts) {
        when(
            weeklyChallengeRepository
                .findPendingWeekStartsBefore(
                    CURRENT_WEEK_START
                )
        ).thenReturn(List.of(weekStarts));
    }
}
