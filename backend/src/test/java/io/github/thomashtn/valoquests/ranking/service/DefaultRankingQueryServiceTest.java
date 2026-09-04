package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse.RankingEntryResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingProgressMapper.WeekBoard;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Verifies what the ranking routes answer, from the rows the recalculation wrote.
 */
@ExtendWith(MockitoExtension.class)
class DefaultRankingQueryServiceTest {

    /**
     * Monday of the current week.
     */
    private static final LocalDate WEEK_START = RankingFixtures.WEEK_START;

    /**
     * Day the queries run on.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 9);

    /**
     * Active player with the lowest identifier.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Second active player.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    /**
     * Player listed without a slot.
     */
    private static final Player CHARLIE = RankingFixtures.player(3, "Charlie", PlayerStatus.INACTIVE);

    @Mock
    private WeeklyPlayerScoreRepository scoreRepository;

    @Mock
    private RankingProgressMapper progressMapper;

    @Mock
    private DailyRankingReader dailyRankingReader;

    @Mock
    private WeeklyTitleResolver titleResolver;

    private DefaultRankingQueryService service;

    @BeforeEach
    void setUp() {
        service = new DefaultRankingQueryService(
            scoreRepository,
            progressMapper,
            dailyRankingReader,
            titleResolver,
            new WeekCalendar(Clock.fixed(RankingFixtures.MIDWEEK, ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Answers the week, its board and each player's honours and variation")
    void shouldAnswerTheCurrentWeek() {
        WeeklyPlayerScore alpha = RankingFixtures.score(ALPHA, 1, 1_200, 300);
        alpha.setPreviousPosition(2);
        WeeklyPlayerScore bravo = RankingFixtures.score(BRAVO, 2, 900, 0);
        bravo.setCalculatedAt(RankingFixtures.MIDWEEK.plusSeconds(60));
        WeeklyPlayerScore charlie = RankingFixtures.score(CHARLIE, null, 0, 0);
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of(alpha, bravo, charlie));
        when(progressMapper.forWeek(WEEK_START, TODAY, List.of(1L, 2L, 3L)))
            .thenReturn(new WeekBoard(5, Map.of()));
        when(titleResolver.resolve(anyList())).thenReturn(Map.of(
            WeeklyTitle.MECHANIC, ALPHA.getId(),
            WeeklyTitle.SCOUT, ALPHA.getId()
        ));

        CurrentRankingResponse response = service.findCurrent();

        assertThat(response.weekStart()).isEqualTo(WEEK_START);
        assertThat(response.weekEnd()).isEqualTo(WEEK_START.plusDays(6));
        assertThat(response.today()).isEqualTo(TODAY);
        assertThat(response.calculatedAt()).isEqualTo(RankingFixtures.MIDWEEK.plusSeconds(60));
        assertThat(response.ranking()).hasSize(3);

        RankingEntryResponse first = response.ranking().getFirst();
        assertThat(first.position()).isEqualTo(1);
        assertThat(first.previousPosition()).isEqualTo(2);
        assertThat(first.positionVariation()).isEqualTo(1);
        assertThat(first.player().displayName()).isEqualTo("Alpha");
        assertThat(first.guardianDamage()).isEqualTo(1_200);
        assertThat(first.challengePoints()).isEqualTo(300);
        assertThat(first.totalPoints()).isEqualTo(1_500);
        assertThat(first.totalChallenges()).isEqualTo(5);
        assertThat(first.titles()).containsExactly(WeeklyTitle.MECHANIC, WeeklyTitle.SCOUT);
        assertThat(first.challengeProgress()).isEmpty();

        RankingEntryResponse last = response.ranking().getLast();
        assertThat(last.position()).isNull();
        assertThat(last.positionVariation()).isZero();
        assertThat(last.titles()).isEmpty();
    }

    @Test
    @DisplayName("Answers an empty week without a calculation instant")
    void shouldAnswerAnEmptyWeek() {
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START)).thenReturn(List.of());
        when(progressMapper.forWeek(WEEK_START, TODAY, List.of())).thenReturn(new WeekBoard(0, Map.of()));
        when(titleResolver.resolve(anyList())).thenReturn(Map.of());

        CurrentRankingResponse response = service.findCurrent();

        assertThat(response.calculatedAt()).isNull();
        assertThat(response.ranking()).isEmpty();
    }

    @Test
    @DisplayName("Names the winner of a finalized week and leaves unranked rows out of it")
    void shouldAnswerAFinalizedWeek() {
        LocalDate lastWeek = WEEK_START.minusWeeks(1);
        Instant finalizedAt = Instant.parse("2026-09-07T00:05:00Z");
        WeeklyPlayerScore bravo = RankingFixtures.score(BRAVO, 1, 2_000, 100);
        bravo.setWeekStart(lastWeek);
        bravo.setFinalizedAt(finalizedAt);
        WeeklyPlayerScore alpha = RankingFixtures.score(ALPHA, 2, 1_000, 0);
        alpha.setWeekStart(lastWeek);
        alpha.setFinalizedAt(finalizedAt);
        WeeklyPlayerScore charlie = RankingFixtures.score(CHARLIE, null, 0, 0);
        charlie.setWeekStart(lastWeek);
        charlie.setFinalizedAt(finalizedAt);

        Page<LocalDate> weekPage = new PageImpl<>(List.of(lastWeek), PageRequest.of(0, 10), 1);
        when(scoreRepository.findFinalizedWeekStarts(PageRequest.of(0, 10))).thenReturn(weekPage);
        when(scoreRepository.findAllByWeekStartInOrderByWeekStartDescPositionAsc(List.of(lastWeek)))
            .thenReturn(List.of(bravo, alpha, charlie));
        when(titleResolver.resolve(List.of(bravo, alpha))).thenReturn(Map.of(WeeklyTitle.REGULAR, BRAVO.getId()));

        PageResponse<RankingHistoryWeekResponse> page = service.findHistory(0, 10);

        assertThat(page.totalElements()).isEqualTo(1);
        RankingHistoryWeekResponse week = page.content().getFirst();
        assertThat(week.weekStart()).isEqualTo(lastWeek);
        assertThat(week.finalizedAt()).isEqualTo(finalizedAt);
        assertThat(week.winnerPlayerId()).isEqualTo(BRAVO.getId());
        assertThat(week.ranking()).hasSize(2);
        assertThat(week.ranking().getFirst().totalPoints()).isEqualTo(2_100);
        assertThat(week.ranking().getFirst().titles()).containsExactly(WeeklyTitle.REGULAR);
        assertThat(week.ranking().getLast().titles()).isEmpty();
    }

    @Test
    @DisplayName("Answers an empty history page without loading any row")
    void shouldAnswerAnEmptyHistoryPage() {
        when(scoreRepository.findFinalizedWeekStarts(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<RankingHistoryWeekResponse> page = service.findHistory(0, 10);

        assertThat(page.content()).isEmpty();
        verify(scoreRepository).findFinalizedWeekStarts(any());
        verifyNoInteractions(titleResolver);
    }

    @Test
    @DisplayName("Rejects pagination outside the public contract")
    void shouldRejectInvalidPagination() {
        assertThatThrownBy(() -> service.findHistory(-1, 10)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Ranks the requested day, or today when none is asked for")
    void shouldDelegateTheDailyBoard() {
        DailyRankingResponse board = new DailyRankingResponse(TODAY, TODAY.minusDays(1), 0, 0, List.of());
        when(dailyRankingReader.read(TODAY)).thenReturn(board);
        when(dailyRankingReader.read(TODAY.minusDays(3))).thenReturn(board);

        assertThat(service.findDaily(null)).isSameAs(board);
        assertThat(service.findDaily(TODAY.minusDays(3))).isSameAs(board);
        verify(dailyRankingReader).read(eq(TODAY));
    }
}
