package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.scoring.model.DailyMatchDamage;
import io.github.thomashtn.valoquests.scoring.service.DailyMatchDamageReader;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link DefaultRankingQueryService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ranking queries")
class DefaultRankingQueryServiceTest {

    /**
     * Monday of the week under test.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 13);

    /**
     * Instant inside that week.
     */
    private static final Instant MIDWEEK = Instant.parse("2026-07-15T12:00:00Z");

    @Mock
    private WeeklyPlayerScoreRepository scoreRepository;

    @Mock
    private PlayerChallengeProgressRepository progressRepository;

    @Mock
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Mock
    private ChallengeDefinitionParser definitionParser;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private DailyMatchDamageReader dailyDamageReader;

    private DefaultRankingQueryService service;

    @BeforeEach
    void setUp() {
        service = new DefaultRankingQueryService(
            scoreRepository,
            progressRepository,
            weeklyChallengeRepository,
            definitionParser,
            playerRepository,
            dailyDamageReader,
            new WeekCalendar(Clock.fixed(MIDWEEK, ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("reports the week the ranking covers as a Monday-to-Sunday span")
    void shouldReportTheWeekAsAMondayToSundaySpan() {
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of());
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of());
        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of());

        CurrentRankingResponse response = service.findCurrent();

        assertThat(response.weekStart()).isEqualTo(WEEK_START);
        assertThat(response.weekEnd()).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(response.ranking()).isEmpty();
        assertThat(response.calculatedAt()).isNull();
    }

    @Test
    @DisplayName("derives position variation from the previous week, zero when there was none")
    void shouldDerivePositionVariationFromThePreviousWeek() {
        WeeklyPlayerScore climbed = score(1L, "climber", 1, 3, 900, 4);
        WeeklyPlayerScore newcomer = score(2L, "newcomer", 2, null, 700, 3);

        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of(climbed, newcomer));
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of());
        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(weeklyChallenge(10L, "First"), weeklyChallenge(11L, "Second")));

        CurrentRankingResponse response = service.findCurrent();

        assertThat(response.ranking())
            .extracting(
                CurrentRankingResponse.RankingEntryResponse::position,
                CurrentRankingResponse.RankingEntryResponse::previousPosition,
                CurrentRankingResponse.RankingEntryResponse::positionVariation,
                CurrentRankingResponse.RankingEntryResponse::totalChallenges
            )
            .containsExactly(
                tuple(1, 3, 2, 2),
                tuple(2, null, 0, 2)
            );
    }

    @Test
    @DisplayName("reports the most recent calculation instant across the ranked players")
    void shouldReportTheMostRecentCalculationInstant() {
        WeeklyPlayerScore first = score(1L, "first", 1, null, 900, 4);
        first.setCalculatedAt(Instant.parse("2026-07-15T06:00:00Z"));
        WeeklyPlayerScore second = score(2L, "second", 2, null, 700, 3);
        second.setCalculatedAt(Instant.parse("2026-07-15T18:00:00Z"));

        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of(first, second));
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of());
        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of());

        assertThat(service.findCurrent().calculatedAt())
            .isEqualTo(Instant.parse("2026-07-15T18:00:00Z"));
    }

    @Test
    @DisplayName("attaches each player's own progress, ordered by challenge, with a display unit")
    void shouldAttachEachPlayersOwnProgressOrderedByChallenge() {
        WeeklyPlayerScore first = score(1L, "first", 1, null, 900, 1);
        WeeklyChallenge kills = weeklyChallenge(11L, "Kill them all");
        WeeklyChallenge days = weeklyChallenge(10L, "Show up");

        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of(first));
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of(
                progress(first.getPlayer(), kills, BigDecimal.valueOf(40), BigDecimal.valueOf(50), false),
                progress(first.getPlayer(), days, BigDecimal.valueOf(3), BigDecimal.valueOf(3), true)
            ));
        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(days, kills));
        when(definitionParser.parse(kills.getChallenge()))
            .thenReturn(definition(ChallengeMetric.KILLS));
        when(definitionParser.parse(days.getChallenge()))
            .thenReturn(definition(ChallengeMetric.PLAY_DAY));

        List<CurrentRankingResponse.ChallengeProgressResponse> progress =
            service.findCurrent().ranking().getFirst().challengeProgress();

        assertThat(progress)
            .extracting(
                CurrentRankingResponse.ChallengeProgressResponse::challengeId,
                CurrentRankingResponse.ChallengeProgressResponse::metric,
                CurrentRankingResponse.ChallengeProgressResponse::unit,
                CurrentRankingResponse.ChallengeProgressResponse::completed
            )
            .containsExactly(
                tuple(10L, "PLAY_DAY", "days", true),
                tuple(11L, "KILLS", "kills", false)
            );
    }

    @Test
    @DisplayName("joins the metrics of a composite challenge and leaves its unit unset")
    void shouldJoinTheMetricsOfACompositeChallenge() {
        WeeklyPlayerScore first = score(1L, "first", 1, null, 900, 0);
        WeeklyChallenge composite = weeklyChallenge(12L, "Do both");

        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START))
            .thenReturn(List.of(first));
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of(
                progress(first.getPlayer(), composite, BigDecimal.ONE, BigDecimal.TEN, false)
            ));
        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(composite));
        when(definitionParser.parse(composite.getChallenge()))
            .thenReturn(new ChallengeDefinition(
                3,
                ProgressMode.ALL,
                List.of(condition(ChallengeMetric.KILLS), condition(ChallengeMetric.ASSISTS))
            ));

        CurrentRankingResponse.ChallengeProgressResponse response =
            service.findCurrent().ranking().getFirst().challengeProgress().getFirst();

        assertThat(response.metric()).isEqualTo("KILLS + ASSISTS");
        assertThat(response.unit()).isNull();
    }

    @Test
    @DisplayName("names the winner of a finalized week and orders it by position")
    void shouldNameTheWinnerOfAFinalizedWeek() {
        WeeklyPlayerScore runnerUp = score(2L, "second", 2, null, 700, 3);
        runnerUp.setWeekStart(WEEK_START);
        runnerUp.setFinalizedAt(Instant.parse("2026-07-20T00:05:00Z"));
        WeeklyPlayerScore winner = score(1L, "first", 1, null, 900, 4);
        winner.setWeekStart(WEEK_START);
        winner.setFinalizedAt(Instant.parse("2026-07-20T00:05:01Z"));

        when(scoreRepository.findFinalizedWeekStarts(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(WEEK_START), PageRequest.of(0, 10), 1));
        when(scoreRepository.findAllByWeekStartInOrderByWeekStartDescPositionAsc(anyList()))
            // Deliberately out of order: the service must not trust the repository's ordering.
            .thenReturn(List.of(runnerUp, winner));

        PageResponse<RankingHistoryWeekResponse> page = service.findHistory(0, 10);

        RankingHistoryWeekResponse week = page.content().getFirst();
        assertThat(week.weekStart()).isEqualTo(WEEK_START);
        assertThat(week.weekEnd()).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(week.winnerPlayerId()).isEqualTo(1L);
        assertThat(week.finalizedAt()).isEqualTo(Instant.parse("2026-07-20T00:05:01Z"));
        assertThat(week.ranking())
            .extracting(RankingHistoryWeekResponse.FinalRankingEntryResponse::position)
            .containsExactly(1, 2);
    }

    @Test
    @DisplayName("returns an empty history page without querying scores")
    void shouldReturnAnEmptyHistoryPageWithoutQueryingScores() {
        when(scoreRepository.findFinalizedWeekStarts(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<RankingHistoryWeekResponse> page = service.findHistory(0, 10);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    @DisplayName("rejects pagination outside the public contract as a caller error")
    void shouldRejectPaginationOutsideThePublicContract() {
        assertThatThrownBy(() -> service.findHistory(-1, 10))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("page");

        assertThatThrownBy(() -> service.findHistory(0, 0))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("size");

        assertThatThrownBy(() -> service.findHistory(0, 101))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("size");
    }

    @Test
    @DisplayName("ranks a day on its own match damage, and states the gap with the day before")
    void shouldRankADayAndStateTheGapWithTheDayBefore() {
        LocalDate today = LocalDate.of(2026, 7, 15);
        LocalDate yesterday = today.minusDays(1);

        givenRoster(rosterPlayer(1L, "climber"), rosterPlayer(2L, "faller"));
        givenDailyDamage(
            Map.of(today, Map.of(1L, 900, 2L, 400), yesterday, Map.of(1L, 550, 2L, 600))
        );

        DailyRankingResponse response = service.findDaily(today);

        assertThat(response.day()).isEqualTo(today);
        assertThat(response.previousDay()).isEqualTo(yesterday);
        assertThat(response.playedPlayerCount()).isEqualTo(2);
        assertThat(response.rosterPlayerCount()).isEqualTo(2);
        assertThat(response.ranking())
            .extracting(
                DailyRankingResponse.DailyRankingEntryResponse::position,
                DailyRankingResponse.DailyRankingEntryResponse::displayName,
                DailyRankingResponse.DailyRankingEntryResponse::matchDamage,
                DailyRankingResponse.DailyRankingEntryResponse::damageVariation
            )
            .containsExactly(
                tuple(1, "climber", 900, 350),
                tuple(2, "faller", 400, -200)
            );
    }

    @Test
    @DisplayName("gives a player who did not play a line at zero rather than no line at all")
    void shouldGiveAPlayerWhoDidNotPlayALineAtZero() {
        LocalDate today = LocalDate.of(2026, 7, 15);

        givenRoster(rosterPlayer(1L, "played"), rosterPlayer(2L, "absent"));
        givenDailyDamage(Map.of(today, Map.of(1L, 300)));

        DailyRankingResponse response = service.findDaily(today);

        assertThat(response.playedPlayerCount()).isEqualTo(1);
        assertThat(response.ranking())
            .extracting(
                DailyRankingResponse.DailyRankingEntryResponse::displayName,
                DailyRankingResponse.DailyRankingEntryResponse::matchDamage
            )
            .containsExactly(tuple("played", 300), tuple("absent", 0));
    }

    @Test
    @DisplayName("never lets a deactivated player consume a slot on the day's board")
    void shouldNeverLetADeactivatedPlayerConsumeADailySlot() {
        LocalDate today = LocalDate.of(2026, 7, 15);

        Player deactivated = rosterPlayer(1L, "deactivated");
        deactivated.setStatus(PlayerStatus.INACTIVE);
        givenRoster(deactivated, rosterPlayer(2L, "active"));
        givenDailyDamage(Map.of(today, Map.of(1L, 1_000, 2L, 200)));

        DailyRankingResponse response = service.findDaily(today);

        assertThat(response.ranking())
            .extracting(
                DailyRankingResponse.DailyRankingEntryResponse::displayName,
                DailyRankingResponse.DailyRankingEntryResponse::position
            )
            .containsExactly(tuple("deactivated", null), tuple("active", 1));
    }

    @Test
    @DisplayName("counts the turnout on the competing squad alone, deactivated players aside")
    void shouldCountTheTurnoutOnTheCompetingSquadAlone() {
        LocalDate today = LocalDate.of(2026, 7, 15);

        Player deactivated = rosterPlayer(1L, "deactivated");
        deactivated.setStatus(PlayerStatus.INACTIVE);
        givenRoster(deactivated, rosterPlayer(2L, "played"), rosterPlayer(3L, "absent"));
        givenDailyDamage(Map.of(today, Map.of(1L, 1_000, 2L, 200)));

        DailyRankingResponse response = service.findDaily(today);

        assertThat(response.playedPlayerCount()).isEqualTo(1);
        assertThat(response.rosterPlayerCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("reports nobody played when the only evening belongs to a deactivated player")
    void shouldReportNobodyPlayedWhenOnlyADeactivatedPlayerPlayed() {
        LocalDate today = LocalDate.of(2026, 7, 15);

        Player deactivated = rosterPlayer(1L, "deactivated");
        deactivated.setStatus(PlayerStatus.INACTIVE);
        givenRoster(deactivated, rosterPlayer(2L, "absent"));
        givenDailyDamage(Map.of(today, Map.of(1L, 1_100)));

        DailyRankingResponse response = service.findDaily(today);

        // The board has no slot to show that evening on, so the turnout must not announce one.
        assertThat(response.playedPlayerCount()).isZero();
        assertThat(response.rosterPlayerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("falls back to today when no day is asked for")
    void shouldFallBackToTodayWhenNoDayIsAskedFor() {
        givenRoster();
        givenDailyDamage(Map.of());

        assertThat(service.findDaily(null).day()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    /**
     * Stubs the roster the day's board draws a line for.
     *
     * @param players players holding a non-archived status
     */
    private void givenRoster(Player... players) {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(players));
    }

    /**
     * Stubs what the shared reader prices each day at, per player.
     *
     * @param weightedByDayAndPlayer weighted damage per day and per player
     */
    private void givenDailyDamage(Map<LocalDate, Map<Long, Integer>> weightedByDayAndPlayer) {
        when(dailyDamageReader.read(any(), any(), any()))
            .thenReturn(new DailyMatchDamage(Map.of(), weightedByDayAndPlayer, Map.of()));
    }

    /**
     * Builds one active rostered player.
     *
     * @param playerId    internal identifier
     * @param displayName name shown on the board
     * @return the player
     */
    private Player rosterPlayer(long playerId, String displayName) {
        Player player = new Player();
        player.setId(playerId);
        player.setDisplayName(displayName);
        player.setStatus(PlayerStatus.ACTIVE);
        return player;
    }

    private WeeklyPlayerScore score(
        long playerId,
        String displayName,
        int position,
        Integer previousPosition,
        int challengeDamage,
        int completedChallenges
    ) {
        Player player = new Player();
        player.setId(playerId);
        player.setDisplayName(displayName);

        WeeklyPlayerScore score = new WeeklyPlayerScore();
        score.setPlayer(player);
        score.setPosition(position);
        score.setPreviousPosition(previousPosition);
        score.setChallengeDamage(challengeDamage);
        score.setCompletedChallenges(completedChallenges);
        return score;
    }

    private WeeklyChallenge weeklyChallenge(long id, String name) {
        Challenge challenge = new Challenge();
        challenge.setName(name);

        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setId(id);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setWeekStart(WEEK_START);
        return weeklyChallenge;
    }

    private PlayerChallengeProgress progress(
        Player player,
        WeeklyChallenge weeklyChallenge,
        BigDecimal currentValue,
        BigDecimal targetValue,
        boolean completed
    ) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCurrentValue(currentValue);
        progress.setTargetValue(targetValue);
        progress.setCompleted(completed);
        return progress;
    }

    private ChallengeDefinition definition(ChallengeMetric metric) {
        return new ChallengeDefinition(
            3,
            ProgressMode.SUM,
            List.of(condition(metric))
        );
    }

    private ChallengeCondition condition(ChallengeMetric metric) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
