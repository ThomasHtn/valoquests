package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.JacksonChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the current challenges read model.
 */
class DefaultChallengeQueryServiceTest {

    /**
     * Current Monday.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Current day, the Wednesday.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    /**
     * Reference the campaign in force is calibrated on.
     */
    private static final int REFERENCE = 5_300;

    /**
     * Latest synchronization time.
     */
    private static final Instant SYNCHRONIZED_AT = Instant.parse("2026-07-22T11:30:00Z");

    /**
     * Weekly selection repository dependency.
     */
    private WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Progress repository dependency.
     */
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Player repository dependency.
     */
    private PlayerRepository playerRepository;

    /**
     * Service under test.
     */
    private DefaultChallengeQueryService service;

    /**
     * Creates the service over mocked repositories and real rules.
     */
    @BeforeEach
    void setUp() {
        weeklyChallengeRepository = mock(WeeklyChallengeRepository.class);
        progressRepository = mock(PlayerChallengeProgressRepository.class);
        playerRepository = mock(PlayerRepository.class);
        ChallengeCalibrationSource calibrationSource = mock(ChallengeCalibrationSource.class);

        when(calibrationSource.forWeek(WEEK_START))
            .thenReturn(new ChallengeCalibration(REFERENCE, 1, ChallengeScaling.NONE));
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of(
                player(1L, PlayerStatus.ACTIVE),
                player(2L, PlayerStatus.ACTIVE),
                player(3L, PlayerStatus.ACTIVE),
                player(4L, PlayerStatus.ACTIVE)
            ));
        when(playerRepository.findLatestSuccessfulSynchronizationAt())
            .thenReturn(Optional.of(SYNCHRONIZED_AT));

        service = new DefaultChallengeQueryService(
            weeklyChallengeRepository,
            progressRepository,
            playerRepository,
            new JacksonChallengeDefinitionParser(JsonMapper.builder().build()),
            new DefaultScoringRuleset(),
            calibrationSource,
            new WeekCalendar(Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that the weekly pack and the week's daily draws are split, priced and counted.
     */
    @Test
    void shouldExposeWeeklyPackAndDailyDrawsWithTheirWorth() {
        WeeklyChallenge hard = weekly(1L, ChallengeDifficulty.HARD, "COMPETITIVE_OR_UNRATED");
        WeeklyChallenge easy = weekly(2L, ChallengeDifficulty.EASY, "COMPETITIVE_OR_UNRATED");
        WeeklyChallenge veryHard = weekly(3L, ChallengeDifficulty.VERY_HARD, "COMPETITIVE");
        WeeklyChallenge monday = daily(4L, WEEK_START);
        WeeklyChallenge today = daily(5L, TODAY);

        when(weeklyChallengeRepository.findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(today, hard, monday, easy, veryHard));
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of(
                progress(easy, player(3L, PlayerStatus.ACTIVE), true),
                progress(easy, player(9L, PlayerStatus.INACTIVE), true),
                progress(easy, player(1L, PlayerStatus.ACTIVE), false),
                progress(today, player(2L, PlayerStatus.ACTIVE), true)
            ));

        CurrentChallengesResponse response = service.findCurrent();

        assertThat(response.weekStart()).isEqualTo(WEEK_START);
        assertThat(response.weekEnd()).isEqualTo(WEEK_START.plusDays(6));
        assertThat(response.today()).isEqualTo(TODAY);
        assertThat(response.lastSuccessfulSynchronizationAt()).isEqualTo(SYNCHRONIZED_AT);
        assertThat(response.roster())
            .extracting(CurrentChallengesResponse.RosterPlayerResponse::id)
            .containsExactly(1L, 2L, 3L, 4L);
        assertThat(response.roster().getFirst().displayName()).isEqualTo("Player 1");

        assertThat(response.challenges())
            .extracting(CurrentChallengesResponse.ChallengeProgressResponse::difficulty)
            .containsExactly(ChallengeDifficulty.EASY, ChallengeDifficulty.HARD, ChallengeDifficulty.VERY_HARD);
        assertThat(response.dailies())
            .extracting(CurrentChallengesResponse.ChallengeProgressResponse::day)
            .containsExactly(WEEK_START, TODAY);

        CurrentChallengesResponse.ChallengeProgressResponse easyEntry = response.challenges().getFirst();
        assertThat(easyEntry.id()).isEqualTo(2L);
        assertThat(easyEntry.cadence()).isEqualTo(ChallengeCadence.WEEKLY);
        assertThat(easyEntry.competitiveOnly()).isFalse();
        assertThat(easyEntry.metric()).isEqualTo("KILLS");
        assertThat(easyEntry.targetValue()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(easyEntry.survivors()).isEqualTo(5);
        assertThat(easyEntry.rankingPoints()).isEqualTo(5);
        // The inactive player's completion never inflates the collective count.
        assertThat(easyEntry.completedPlayers()).isEqualTo(1);
        assertThat(easyEntry.totalPlayers()).isEqualTo(4);
        assertThat(easyEntry.completedPlayerIds()).containsExactly(3L);
        assertThat(easyEntry.completionPercentage()).isEqualByComparingTo(BigDecimal.valueOf(25));

        assertThat(response.challenges().getLast().competitiveOnly()).isTrue();
        assertThat(response.challenges().getLast().survivors()).isEqualTo(29);

        CurrentChallengesResponse.ChallengeProgressResponse todayEntry = response.dailies().getLast();
        assertThat(todayEntry.cadence()).isEqualTo(ChallengeCadence.DAILY);
        assertThat(todayEntry.difficulty()).isNull();
        assertThat(todayEntry.survivors()).isEqualTo(6);
        assertThat(todayEntry.rankingPoints()).isEqualTo(6);
        assertThat(todayEntry.completedPlayers()).isEqualTo(1);
        assertThat(todayEntry.completedPlayerIds()).containsExactly(2L);
    }

    /**
     * Verifies that a week without any draw yields empty lists rather than failing.
     */
    @Test
    void shouldExposeEmptyListsBeforeAnyDraw() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of());

        CurrentChallengesResponse response = service.findCurrent();

        assertThat(response.roster()).isEmpty();
        assertThat(response.challenges()).isEmpty();
        assertThat(response.dailies()).isEmpty();
    }

    /**
     * Creates a player fixture.
     *
     * @param id     player identifier
     * @param status lifecycle status
     * @return player fixture
     */
    private Player player(long id, PlayerStatus status) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName("Player " + id);
        player.setStatus(status);
        return player;
    }

    /**
     * Creates a weekly selection of a kill-count challenge resolved to three matches of ten kills.
     *
     * @param id         selection identifier
     * @param difficulty difficulty tier
     * @param gameMode   game-mode filter of the resolved condition
     * @return weekly selection fixture
     */
    private WeeklyChallenge weekly(long id, ChallengeDifficulty difficulty, String gameMode) {
        Challenge challenge = new Challenge();
        challenge.setId(id * 10);
        challenge.setCode(difficulty + "_KILL_GAMES");
        challenge.setName("Kill games");
        challenge.setDescription("Finish matches with kills.");
        challenge.setDifficulty(difficulty);
        challenge.setProgressMode(ProgressMode.COUNT_MATCHES);
        challenge.setSchemaVersion(3);
        challenge.setConditionsJson(
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":10,\"gameMode\":\"" + gameMode
                + "\",\"occurrences\":3,\"scope\":\"PER_MATCH\"}]"
        );

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setId(id);
        selection.setWeekStart(WEEK_START);
        selection.setChallenge(challenge);
        selection.setResolvedConditionsJson(
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":10,\"gameMode\":\"" + gameMode
                + "\",\"occurrences\":3,\"scope\":\"PER_MATCH\"}]"
        );
        return selection;
    }

    /**
     * Creates a daily selection of a one-match challenge.
     *
     * @param id  selection identifier
     * @param day covered day
     * @return daily selection fixture
     */
    private WeeklyChallenge daily(long id, LocalDate day) {
        Challenge challenge = new Challenge();
        challenge.setId(id * 10);
        challenge.setCode("DAILY_ONE_LONG");
        challenge.setName("One long match");
        challenge.setDescription("Play one long match.");
        challenge.setCadence(ChallengeCadence.DAILY);
        challenge.setProgressMode(ProgressMode.SUM);
        challenge.setSchemaVersion(3);
        challenge.setConditionsJson(
            "[{\"metric\":\"MATCHES_PLAYED\",\"operator\":\"GTE\",\"target\":1,"
                + "\"gameMode\":\"COMPETITIVE_OR_UNRATED\"}]"
        );

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setId(id);
        selection.setWeekStart(WEEK_START);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(day);
        selection.setChallenge(challenge);
        selection.setResolvedConditionsJson(
            "[{\"metric\":\"MATCHES_PLAYED\",\"operator\":\"GTE\",\"target\":1,"
                + "\"gameMode\":\"COMPETITIVE_OR_UNRATED\"}]"
        );
        return selection;
    }

    /**
     * Creates one progress row.
     *
     * @param selection evaluated selection
     * @param player    player who owns the row
     * @param completed whether the challenge is completed
     * @return progress fixture
     */
    private PlayerChallengeProgress progress(
        WeeklyChallenge selection,
        Player player,
        boolean completed
    ) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(selection);
        progress.setCompleted(completed);
        return progress;
    }
}
