package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.JacksonChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.scoring.service.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
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
    private ChallengeSelectionRepository challengeSelectionRepository;

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
        challengeSelectionRepository = mock(ChallengeSelectionRepository.class);
        progressRepository = mock(PlayerChallengeProgressRepository.class);
        playerRepository = mock(PlayerRepository.class);
        ChallengeCalibrationSource calibrationSource = mock(ChallengeCalibrationSource.class);

        when(calibrationSource.forWeek(WEEK_START))
            .thenReturn(new ChallengeCalibration(REFERENCE, 1, CampaignDifficulty.AMATEUR));
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
            challengeSelectionRepository,
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
        ChallengeSelection hard = weekly(1L, ChallengeTier.HARD, "COMPETITIVE_OR_UNRATED");
        ChallengeSelection easy = weekly(2L, ChallengeTier.EASY, "COMPETITIVE_OR_UNRATED");
        ChallengeSelection veryHard = weekly(3L, ChallengeTier.VERY_HARD, "COMPETITIVE");
        ChallengeSelection monday = daily(4L, WEEK_START);
        ChallengeSelection today = daily(5L, TODAY);

        when(challengeSelectionRepository.findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(today, hard, monday, easy, veryHard));
        when(progressRepository
            .findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(WEEK_START))
            .thenReturn(List.of(
                progress(easy, player(3L, PlayerStatus.ACTIVE), true, 3),
                progress(easy, player(9L, PlayerStatus.INACTIVE), true, 3),
                progress(easy, player(1L, PlayerStatus.ACTIVE), false, 1),
                progress(monday, player(4L, PlayerStatus.ACTIVE), false, 2),
                progress(today, player(2L, PlayerStatus.ACTIVE), true, 1)
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
        assertThat(response.roster().getFirst().portrait()).isEqualTo("Agent 1");

        assertThat(response.challenges())
            .extracting(CurrentChallengesResponse.ChallengeProgressResponse::tier)
            .containsExactly(ChallengeTier.EASY, ChallengeTier.HARD, ChallengeTier.VERY_HARD);
        assertThat(response.dailies())
            .extracting(CurrentChallengesResponse.ChallengeProgressResponse::day)
            .containsExactly(WEEK_START, TODAY);

        CurrentChallengesResponse.ChallengeProgressResponse easyEntry = response.challenges().getFirst();
        assertThat(easyEntry.id()).isEqualTo(2L);
        assertThat(easyEntry.cadence()).isEqualTo(ChallengeCadence.WEEKLY);
        assertThat(easyEntry.metric()).isEqualTo("KILLS");
        assertThat(easyEntry.targetValue()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(easyEntry.survivors()).isEqualTo(5);
        // The inactive player's completion never inflates the collective count.
        assertThat(easyEntry.completedPlayerIds()).containsExactly(3L);
        // One line per active player in roster order, zero for those not evaluated yet.
        assertThat(easyEntry.players())
            .extracting(CurrentChallengesResponse.PlayerProgressResponse::playerId)
            .containsExactly(1L, 2L, 3L, 4L);
        assertThat(easyEntry.players())
            .extracting(line -> line.currentValue().intValue())
            .containsExactly(1, 0, 3, 0);
        assertThat(easyEntry.players())
            .extracting(CurrentChallengesResponse.PlayerProgressResponse::completed)
            .containsExactly(false, false, true, false);

        assertThat(response.challenges().getLast().survivors()).isEqualTo(29);

        // A past day's challenge keeps each player's progress too.
        CurrentChallengesResponse.ChallengeProgressResponse mondayEntry = response.dailies().getFirst();
        assertThat(mondayEntry.players().getLast().currentValue()).isEqualByComparingTo(BigDecimal.valueOf(2));

        CurrentChallengesResponse.ChallengeProgressResponse todayEntry = response.dailies().getLast();
        assertThat(todayEntry.cadence()).isEqualTo(ChallengeCadence.DAILY);
        assertThat(todayEntry.tier()).isNull();
        assertThat(todayEntry.survivors()).isEqualTo(6);
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
        player.setPortrait("Agent " + id);
        return player;
    }

    /**
     * Creates a weekly selection of a kill-count challenge resolved to three matches of ten kills.
     *
     * @param id         selection identifier
     * @param tier tier
     * @param gameMode   game-mode filter of the resolved condition
     * @return weekly selection fixture
     */
    private ChallengeSelection weekly(long id, ChallengeTier tier, String gameMode) {
        Challenge challenge = new Challenge();
        challenge.setId(id * 10);
        challenge.setCode(tier + "_KILL_GAMES");
        challenge.setName("Kill games");
        challenge.setDescription("Finish matches with kills.");
        challenge.setTier(tier);
        challenge.setProgressMode(ProgressMode.COUNT_MATCHES);
        challenge.setSchemaVersion(3);
        challenge.setAmateurConditionsJson(
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":10,\"gameMode\":\"" + gameMode
                + "\",\"occurrences\":3,\"scope\":\"PER_MATCH\"}]"
        );
        challenge.setProConditionsJson(challenge.getAmateurConditionsJson());

        ChallengeSelection selection = new ChallengeSelection();
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
    private ChallengeSelection daily(long id, LocalDate day) {
        Challenge challenge = new Challenge();
        challenge.setId(id * 10);
        challenge.setCode("DAILY_ONE_LONG");
        challenge.setName("One long match");
        challenge.setDescription("Play one long match.");
        challenge.setCadence(ChallengeCadence.DAILY);
        challenge.setProgressMode(ProgressMode.SUM);
        challenge.setSchemaVersion(3);
        challenge.setAmateurConditionsJson(
            "[{\"metric\":\"MATCHES_PLAYED\",\"operator\":\"GTE\",\"target\":1,"
                + "\"gameMode\":\"COMPETITIVE_OR_UNRATED\"}]"
        );
        challenge.setProConditionsJson(challenge.getAmateurConditionsJson());

        ChallengeSelection selection = new ChallengeSelection();
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
     * @param value     progress so far
     * @return progress fixture
     */
    private PlayerChallengeProgress progress(
        ChallengeSelection selection,
        Player player,
        boolean completed,
        int value
    ) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setSelection(selection);
        progress.setCompleted(completed);
        progress.setCurrentValue(BigDecimal.valueOf(value));
        return progress;
    }
}
