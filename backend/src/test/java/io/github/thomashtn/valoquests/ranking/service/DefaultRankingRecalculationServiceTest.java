package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.ChallengePointsReader.ChallengeTally;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies how a week is added up and ordered, and who takes a slot in it.
 */
@ExtendWith(MockitoExtension.class)
class DefaultRankingRecalculationServiceTest {

    /**
     * Monday of the week being rebuilt.
     */
    private static final LocalDate WEEK_START = RankingFixtures.WEEK_START;

    /**
     * Active player with the lowest identifier.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Second active player.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    /**
     * Player who measures themselves against the squad without taking part.
     */
    private static final Player CHARLIE = RankingFixtures.player(3, "Charlie", PlayerStatus.INACTIVE);

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private WeeklyPlayerScoreRepository scoreRepository;

    @Mock
    private DailyOutputReader dailyOutputReader;

    @Mock
    private ChallengePointsReader challengePointsReader;

    private DefaultRankingRecalculationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(RankingFixtures.MIDWEEK, ZoneOffset.UTC);
        service = new DefaultRankingRecalculationService(
            playerRepository,
            scoreRepository,
            dailyOutputReader,
            challengePointsReader,
            clock,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Adds the week's damage to its challenge points and orders on the sum")
    void shouldSumTheWeekAndOrderOnTotalPoints() {
        givenRoster(ALPHA, BRAVO);
        givenOutput(RankingFixtures.output(Map.of(
            ALPHA.getId(), Map.of(
                WEEK_START, RankingFixtures.dayOutput(500, 1, 1),
                WEEK_START.plusDays(1), RankingFixtures.dayOutput(700, 2, 2)
            ),
            BRAVO.getId(), Map.of(
                WEEK_START.plusDays(3), RankingFixtures.dayOutput(1_500, 3, 4)
            )
        )));
        when(challengePointsReader.read(WEEK_START)).thenReturn(Map.of(
            ALPHA.getId(), new ChallengeTally(300, 2, 1)
        ));

        List<WeeklyPlayerScore> scores = recalculate();

        assertThat(scores).hasSize(2);

        WeeklyPlayerScore bravo = scores.get(0);
        assertThat(bravo.getPlayer()).isSameAs(BRAVO);
        assertThat(bravo.getPosition()).isEqualTo(1);
        assertThat(bravo.getGuardianDamage()).isEqualTo(1_500);
        assertThat(bravo.getFood()).isEqualTo(450);
        assertThat(bravo.getComponents()).isEqualTo(1_050);
        assertThat(bravo.getMatchCount()).isEqualTo(3);
        assertThat(bravo.getActiveDays()).isEqualTo(1);
        assertThat(bravo.getStreakDays()).isEqualTo(4);
        assertThat(bravo.getChallengePoints()).isZero();
        assertThat(bravo.getTotalPoints()).isEqualTo(1_500);

        WeeklyPlayerScore alpha = scores.get(1);
        assertThat(alpha.getPosition()).isEqualTo(2);
        assertThat(alpha.getGuardianDamage()).isEqualTo(1_200);
        assertThat(alpha.getActiveDays()).isEqualTo(2);
        assertThat(alpha.getStreakDays()).isEqualTo(2);
        assertThat(alpha.getChallengePoints()).isEqualTo(300);
        assertThat(alpha.getCompletedChallenges()).isEqualTo(2);
        assertThat(alpha.getCompletedDailyChallenges()).isEqualTo(1);
        assertThat(alpha.getTotalPoints()).isEqualTo(1_500);
        assertThat(alpha.getCalculatedAt()).isEqualTo(RankingFixtures.MIDWEEK);
        assertThat(alpha.getPreviousPosition()).isNull();
    }

    @Test
    @DisplayName("Breaks a tie on guardian damage, then on validated challenges, then on identifier")
    void shouldBreakTiesInDocumentedOrder() {
        givenRoster(ALPHA, BRAVO);
        givenOutput(RankingFixtures.output(Map.of(
            ALPHA.getId(), Map.of(WEEK_START, RankingFixtures.dayOutput(1_000, 1, 1)),
            BRAVO.getId(), Map.of(WEEK_START, RankingFixtures.dayOutput(1_000, 1, 1))
        )));
        when(challengePointsReader.read(WEEK_START)).thenReturn(Map.of(
            ALPHA.getId(), new ChallengeTally(100, 1, 0),
            BRAVO.getId(), new ChallengeTally(100, 0, 2)
        ));

        List<WeeklyPlayerScore> scores = recalculate();

        // Same total, same damage: Bravo validated more challenges, so Bravo is first.
        assertThat(scores).extracting(score -> score.getPlayer().getId()).containsExactly(2L, 1L);
        assertThat(scores).extracting(WeeklyPlayerScore::getPosition).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Lists an inactive player with their counts only: no damage, no points, no slot")
    void shouldKeepAnInactivePlayerOutOfTheCompetition() {
        givenRoster(ALPHA, CHARLIE);
        givenOutput(RankingFixtures.output(Map.of(
            ALPHA.getId(), Map.of(WEEK_START, RankingFixtures.dayOutput(400, 1, 1))
        )));
        when(challengePointsReader.read(WEEK_START)).thenReturn(Map.of(
            CHARLIE.getId(), new ChallengeTally(500, 3, 2)
        ));

        List<WeeklyPlayerScore> scores = recalculate();

        // Only the competing squad's matches are priced: the reader is never asked for Charlie's.
        verify(dailyOutputReader).read(eq(EnumSet.of(PlayerStatus.ACTIVE)), any(), any());

        WeeklyPlayerScore charlie = scores.stream()
            .filter(score -> score.getPlayer() == CHARLIE)
            .findFirst()
            .orElseThrow();
        assertThat(charlie.getPosition()).isNull();
        assertThat(charlie.getGuardianDamage()).isZero();
        assertThat(charlie.getChallengePoints()).isZero();
        assertThat(charlie.getTotalPoints()).isZero();
        assertThat(charlie.getCompletedChallenges()).isEqualTo(3);
        assertThat(charlie.getCompletedDailyChallenges()).isEqualTo(2);

        WeeklyPlayerScore alpha = scores.stream()
            .filter(score -> score.getPlayer() == ALPHA)
            .findFirst()
            .orElseThrow();
        assertThat(alpha.getPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("Keeps the former position of an existing row and reuses the row")
    void shouldPreserveThePreviousPositionOfAnExistingRow() {
        givenRoster(ALPHA, BRAVO);
        WeeklyPlayerScore existing = RankingFixtures.score(ALPHA, 2, 0, 0);
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START)).thenReturn(List.of(existing));
        givenOutput(RankingFixtures.output(Map.of(
            ALPHA.getId(), Map.of(WEEK_START, RankingFixtures.dayOutput(900, 1, 1))
        )));
        when(challengePointsReader.read(WEEK_START)).thenReturn(Map.of());

        List<WeeklyPlayerScore> scores = recalculate();

        WeeklyPlayerScore alpha = scores.get(0);
        assertThat(alpha).isSameAs(existing);
        assertThat(alpha.getPosition()).isEqualTo(1);
        assertThat(alpha.getPreviousPosition()).isEqualTo(2);
        assertThat(scores.get(1).getPreviousPosition()).isNull();
    }

    @Test
    @DisplayName("Clears the week when nobody is tracked any more")
    void shouldClearTheWeekWithoutPlayers() {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED)).thenReturn(List.of());

        service.recalculateWeek(WEEK_START);

        verify(scoreRepository).deleteAllByWeekStart(WEEK_START);
        verify(scoreRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Refuses a week that does not start on a Monday")
    void shouldRejectANonMonday() {
        assertThatThrownBy(() -> service.recalculateWeek(WEEK_START.plusDays(1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rebuilds the current week when asked for the current ranking")
    void shouldResolveTheCurrentWeek() {
        givenRoster(ALPHA);
        givenOutput(RankingFixtures.output(Map.of()));
        when(challengePointsReader.read(WEEK_START)).thenReturn(Map.of());

        service.recalculateCurrentRanking();

        verify(scoreRepository).deleteAllByWeekStartAndPlayerIdNotIn(WEEK_START, List.of(ALPHA.getId()));
    }

    private void givenRoster(Player... players) {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED)).thenReturn(List.of(players));
    }

    private void givenOutput(DailyOutput output) {
        when(dailyOutputReader.read(any(), eq(WEEK_START), eq(WEEK_START.plusDays(6)))).thenReturn(output);
    }

    @SuppressWarnings("unchecked")
    private List<WeeklyPlayerScore> recalculate() {
        service.recalculateWeek(WEEK_START);

        ArgumentCaptor<List<WeeklyPlayerScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(scoreRepository).saveAll(captor.capture());

        return captor.getValue();
    }
}
