package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.dto.PlayerProgressionResponse;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultPlayerProgressionQueryService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Player progression analytics")
class DefaultPlayerProgressionQueryServiceTest {

    /**
     * Identifier of the player every test queries.
     */
    private static final long PLAYER_ID = 1L;

    /**
     * A Monday, so every date derived from it has a predictable day of the week.
     */
    private static final String MONDAY = "2026-08-03";

    /**
     * Mocked player repository.
     */
    @Mock
    private PlayerRepository playerRepository;

    /**
     * Mocked player-match repository.
     */
    @Mock
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Mocked week calendar, owning the zone weekdays and time slots are resolved in.
     */
    @Mock
    private WeekCalendar weekCalendar;

    /**
     * Seasons handed out by {@link #season(long)}, so matches of one season share one instance.
     */
    private final Map<Long, Season> seasons = new HashMap<>();

    /**
     * Service under test.
     */
    private DefaultPlayerProgressionQueryService service;

    /**
     * Creates the service under test before each test.
     *
     * <p>The zone is stubbed leniently: a test working on an empty history never reads a match's
     * calendar day, so the stub legitimately goes unused there.
     */
    @BeforeEach
    void setUp() {
        lenient().when(weekCalendar.zone()).thenReturn(ZoneId.of("UTC"));
        lenient().when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        service = new DefaultPlayerProgressionQueryService(
            playerRepository, playerMatchRepository, weekCalendar
        );
    }

    @Test
    @DisplayName("restricts every figure to the selected seasons")
    void shouldRestrictAnalyticsToTheSelectedSeasons() {
        givenHistory(
            competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN),
            competitive(2L, "2026-11-02T10:00:00Z", MatchResult.LOSS)
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, List.of(1L));

        assertThat(response.evolution()).singleElement()
            .satisfies(season -> assertThat(season.seasonId()).isEqualTo(1L));
        assertThat(response.records().longestWinStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("covers every season when none is selected, oldest first")
    void shouldCoverEverySeasonWhenNoneIsSelected() {
        givenHistory(
            competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN),
            competitive(2L, "2026-11-02T10:00:00Z", MatchResult.LOSS)
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.evolution())
            .extracting(PlayerProgressionResponse.SeasonEvolution::seasonId)
            .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("keeps non-competitive matches out of every figure but the active-day streak")
    void shouldExcludeNonCompetitiveMatchesExceptFromTheDayStreak() {
        PlayerMatch deathmatch = competitive(1L, "2026-08-04T10:00:00Z", MatchResult.WIN);
        deathmatch.getMatch().setGameMode(GameMode.DEATHMATCH);
        givenHistory(
            competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN),
            deathmatch,
            competitive(1L, "2026-08-05T10:00:00Z", MatchResult.WIN)
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.evolution().getFirst().points()).hasSize(2);
        assertThat(response.records().longestActiveDayStreak()).isEqualTo(3);
    }

    @Test
    @DisplayName("breaks the active-day streak on a day without a match")
    void shouldBreakTheActiveDayStreakOnAMissingDay() {
        givenHistory(
            competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN),
            competitive(1L, "2026-08-04T10:00:00Z", MatchResult.WIN),
            competitive(1L, "2026-08-06T10:00:00Z", MatchResult.WIN),
            competitive(1L, "2026-08-06T22:00:00Z", MatchResult.WIN)
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.records().longestActiveDayStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("crowns the best weekday only among days holding enough matches")
    void shouldFlagTheBestWeekdayOnlyWhenEnoughMatchesBackIt() {
        List<PlayerMatch> history = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            history.add(competitive(1L, MONDAY + "T1" + index + ":00:00Z", MatchResult.WIN));
        }
        for (int index = 0; index < 5; index++) {
            MatchResult result = index < 3 ? MatchResult.WIN : MatchResult.LOSS;
            history.add(competitive(1L, "2026-08-04T1" + index + ":00:00Z", result));
        }
        givenHistory(history.toArray(new PlayerMatch[0]));

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.weekdays()).hasSize(7);
        assertThat(response.weekdays()).filteredOn(PlayerProgressionResponse.WeekdayPerformance::best)
            .singleElement()
            .satisfies(day -> assertThat(day.day()).isEqualTo(DayOfWeek.TUESDAY));
        assertThat(response.weekdays().getFirst().winRate()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("crowns no time slot when none holds enough matches")
    void shouldFlagNoBestSlotWhenNoSlotClearsTheSampleThreshold() {
        givenHistory(
            competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN),
            competitive(1L, MONDAY + "T11:00:00Z", MatchResult.WIN)
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.hourSlots()).hasSize(8);
        assertThat(response.hourSlots()).noneMatch(PlayerProgressionResponse.HourSlotPerformance::best);
        assertThat(response.hourSlots().get(3).startHour()).isEqualTo(9);
        assertThat(response.hourSlots().get(3).matchesPlayed()).isEqualTo(2);
    }

    @Test
    @DisplayName("crowns the best time slot once it holds enough matches")
    void shouldFlagTheBestTimeSlot() {
        List<PlayerMatch> history = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            history.add(competitive(1L, MONDAY + "T21:" + index + "0:00Z", MatchResult.WIN));
        }
        givenHistory(history.toArray(new PlayerMatch[0]));

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.hourSlots()).filteredOn(PlayerProgressionResponse.HourSlotPerformance::best)
            .singleElement()
            .satisfies(slot -> assertThat(slot.startHour()).isEqualTo(21));
    }

    @Test
    @DisplayName("keeps short matches out of the headshot record")
    void shouldIgnoreShortMatchesWhenElectingTheHeadshotRecord() {
        PlayerMatch remade = competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN);
        remade.setRoundsPlayed(3);
        remade.setHeadshots(10);
        remade.setBodyshots(0);
        remade.setLegshots(0);

        PlayerMatch full = competitive(1L, "2026-08-04T10:00:00Z", MatchResult.WIN);
        full.setRoundsPlayed(24);
        full.setHeadshots(30);
        full.setBodyshots(70);
        full.setLegshots(0);
        full.getMatch().setMapName("Bind");

        givenHistory(remade, full);

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.records().bestHeadshotPercentage().value()).isEqualByComparingTo("30.00");
        assertThat(response.records().bestHeadshotPercentage().mapName()).isEqualTo("Bind");
    }

    @Test
    @DisplayName("steps over a remake without breaking the win streak")
    void shouldSkipRemakesWithoutBreakingTheWinStreak() {
        givenHistory(
            competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN),
            competitive(1L, MONDAY + "T11:00:00Z", MatchResult.REMAKE),
            competitive(1L, MONDAY + "T12:00:00Z", MatchResult.WIN),
            competitive(1L, MONDAY + "T13:00:00Z", MatchResult.LOSS),
            competitive(1L, MONDAY + "T14:00:00Z", MatchResult.WIN)
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.records().longestWinStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("reports where hits landed over the whole filtered set")
    void shouldReportTheAimBreakdown() {
        PlayerMatch first = competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN);
        first.setHeadshots(20);
        first.setBodyshots(70);
        first.setLegshots(10);
        PlayerMatch second = competitive(1L, MONDAY + "T11:00:00Z", MatchResult.WIN);
        second.setHeadshots(30);
        second.setBodyshots(60);
        second.setLegshots(10);
        givenHistory(first, second);

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.aim().totalShots()).isEqualTo(200);
        assertThat(response.aim().headPercentage()).isEqualByComparingTo("25.00");
        assertThat(response.aim().bodyPercentage()).isEqualByComparingTo("65.00");
        assertThat(response.aim().legPercentage()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("reports the peak rank, the MVP count and the per-match bests")
    void shouldReportPersonalBests() {
        PlayerMatch first = competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN);
        first.setCompetitiveTier(CompetitiveTier.GOLD_2);
        first.setMvp(true);
        PlayerMatch second = competitive(1L, MONDAY + "T11:00:00Z", MatchResult.WIN);
        second.setCompetitiveTier(CompetitiveTier.PLATINUM_1);
        second.setKills(25);
        second.setDamageDealt(4200);
        second.setAcs(BigDecimal.valueOf(320));
        givenHistory(first, second);

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.records().peakTier()).isEqualTo(CompetitiveTier.PLATINUM_1);
        assertThat(response.records().mvps()).isEqualTo(1);
        assertThat(response.records().mostKills().value()).isEqualByComparingTo("25");
        assertThat(response.records().mostDamage().value()).isEqualByComparingTo("4200");
        assertThat(response.records().bestAcs().value()).isEqualByComparingTo("320");
    }

    @Test
    @DisplayName("reports no record rather than a record of zero")
    void shouldReportNoRecordWhenNothingQualifies() {
        PlayerMatch blank = competitive(1L, MONDAY + "T10:00:00Z", MatchResult.LOSS);
        blank.setKills(0);
        blank.setAssists(0);
        blank.setDamageDealt(0);
        blank.setAcs(null);
        blank.setHeadshots(0);
        blank.setBodyshots(0);
        blank.setLegshots(0);
        givenHistory(blank);

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.records().mostKills()).isNull();
        assertThat(response.records().bestAcs()).isNull();
        assertThat(response.records().mostDamage()).isNull();
        assertThat(response.records().bestKda()).isNull();
        assertThat(response.records().bestHeadshotPercentage()).isNull();
        assertThat(response.records().peakTier()).isNull();
    }

    @Test
    @DisplayName("returns an empty but complete payload for a player with no match")
    void shouldReturnEmptyAnalyticsForAPlayerWithNoMatch() {
        givenHistory();

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, List.of(1L));

        assertThat(response.evolution()).isEmpty();
        assertThat(response.maps()).isEmpty();
        assertThat(response.agents()).isEmpty();
        assertThat(response.weekdays()).hasSize(7).noneMatch(PlayerProgressionResponse.WeekdayPerformance::best);
        assertThat(response.hourSlots()).hasSize(8);
        assertThat(response.aim().totalShots()).isZero();
        assertThat(response.records().longestActiveDayStreak()).isZero();
        assertThat(response.records().longestWinStreak()).isZero();
    }

    @Test
    @DisplayName("aggregates maps and agents, most played first")
    void shouldAggregateMapsAndAgentsMostPlayedFirst() {
        PlayerMatch bind = competitive(1L, MONDAY + "T10:00:00Z", MatchResult.WIN);
        bind.getMatch().setMapName("Bind");
        bind.setAgentName("Reyna");
        givenHistory(
            competitive(1L, MONDAY + "T11:00:00Z", MatchResult.WIN),
            competitive(1L, MONDAY + "T12:00:00Z", MatchResult.LOSS),
            bind
        );

        PlayerProgressionResponse response = service.findByPlayerId(PLAYER_ID, null);

        assertThat(response.maps()).extracting("mapName").containsExactly("Ascent", "Bind");
        assertThat(response.maps().getFirst().winRate()).isEqualByComparingTo("50.00");
        assertThat(response.agents()).extracting("agentName").containsExactly("Jett", "Reyna");
    }

    @Test
    @DisplayName("fails fast on an untracked player")
    void shouldThrowWhenPlayerDoesNotExist() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.findByPlayerId(PLAYER_ID, null))
            .isInstanceOf(PlayerNotFoundException.class);
    }

    private void givenHistory(PlayerMatch... matches) {
        when(playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(PLAYER_ID))
            .thenReturn(List.of(matches));
    }

    private Season season(long id) {
        return seasons.computeIfAbsent(id, key -> {
            Season season = new Season();
            season.setId(key);
            season.setName("Episode " + key);
            season.setActive(key == 1L);
            return season;
        });
    }

    private PlayerMatch competitive(long seasonId, String startedAt, MatchResult result) {
        ValorantMatch valorantMatch = new ValorantMatch();
        valorantMatch.setSeason(season(seasonId));
        valorantMatch.setStartedAt(Instant.parse(startedAt));
        valorantMatch.setGameMode(GameMode.COMPETITIVE);
        valorantMatch.setMapName("Ascent");

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(valorantMatch);
        playerMatch.setAgentName("Jett");
        playerMatch.setResult(result);
        playerMatch.setKills(15);
        playerMatch.setDeaths(10);
        playerMatch.setAssists(5);
        playerMatch.setHeadshots(20);
        playerMatch.setBodyshots(75);
        playerMatch.setLegshots(5);
        playerMatch.setDamageDealt(3000);
        playerMatch.setRoundsPlayed(20);
        playerMatch.setAcs(BigDecimal.valueOf(230));
        playerMatch.setAdr(BigDecimal.valueOf(150));
        return playerMatch;
    }
}
