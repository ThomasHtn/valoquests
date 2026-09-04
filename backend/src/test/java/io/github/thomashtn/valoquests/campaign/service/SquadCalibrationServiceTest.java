package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.PlayerCalibration;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * Verifies the one measurement a campaign is sized on, and can never revise.
 *
 * <p>Every total is chosen so its weekly average divides exactly: the nine-month window ending on
 * 04/09/2026 is 275 days, so 27 500 damage is exactly 700 a week.
 */
@ExtendWith(MockitoExtension.class)
class SquadCalibrationServiceTest {

    /**
     * Day the calibration is taken on.
     */
    private static final LocalDate REFERENCE_DAY = LocalDate.of(2026, 9, 4);

    /**
     * First day of the untouched nine-month window.
     */
    private static final LocalDate NINE_MONTH_START = REFERENCE_DAY.minusMonths(9);

    /**
     * Damage totals averaging 700, 2 800 and 7 000 a week over that window.
     */
    private static final int LIGHT_TOTAL = 27_500;

    /**
     * Damage total averaging 2 800 a week.
     */
    private static final int MEDIUM_TOTAL = 110_000;

    /**
     * Damage total averaging 7 000 a week.
     */
    private static final int HEAVY_TOTAL = 275_000;

    @Mock
    private PlayerMatchRepository playerMatchRepository;

    @Mock
    private DailyOutputReader dailyOutputReader;

    private SquadCalibrationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-04T12:00:00Z"), ZoneOffset.UTC);
        service = new SquadCalibrationService(
            playerMatchRepository,
            dailyOutputReader,
            new DefaultScoringRuleset(),
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Averages the players' weekly averages over the whole nine-month window")
    void shouldAverageTheWeeklyAverages() {
        Player light = player(1, "Alpha");
        Player medium = player(2, "Bravo");
        Player heavy = player(3, "Charlie");

        covered(light, medium, heavy);
        produces(light, LIGHT_TOTAL);
        produces(medium, MEDIUM_TOTAL);
        produces(heavy, HEAVY_TOTAL);
        noMatches(light, medium, heavy);

        SquadCalibration calibration = service.calibrate(List.of(light, medium, heavy), REFERENCE_DAY);

        assertThat(calibration.reference()).isEqualTo(3_500);
        assertThat(calibration.tier()).isEqualTo(CampaignTier.NORMAL);
        assertThat(calibration.windowMonths()).isEqualTo(9);
        assertThat(calibration.firstDay()).isEqualTo(NINE_MONTH_START);
        assertThat(calibration.players())
            .extracting(PlayerCalibration::weeklyAverage)
            .containsExactly(700, 2_800, 7_000);
    }

    @Test
    @DisplayName("Never drops the reference below the floor")
    void shouldApplyTheReferenceFloor() {
        Player quiet = player(1, "Alpha");

        covered(quiet);
        produces(quiet, LIGHT_TOTAL);
        noMatches(quiet);

        SquadCalibration calibration = service.calibrate(List.of(quiet), REFERENCE_DAY);

        assertThat(calibration.reference()).isEqualTo(2_000);
        assertThat(calibration.tier()).isEqualTo(CampaignTier.AMATEUR);
    }

    @Test
    @DisplayName("Shrinks the window a month at a time, for everyone at once")
    void shouldShrinkTheWindowUntilEveryoneIsCovered() {
        Player veteran = player(1, "Alpha");
        Player recent = player(2, "Bravo");

        when(playerMatchRepository.findEarliestMatchStartedAt(1L))
            .thenReturn(java.util.Optional.of(instant(NINE_MONTH_START.minusMonths(2))));
        when(playerMatchRepository.findEarliestMatchStartedAt(2L))
            .thenReturn(java.util.Optional.of(instant(REFERENCE_DAY.minusMonths(4))));
        producesAnyWindow(veteran, 0);
        producesAnyWindow(recent, 0);
        noMatches(veteran, recent);

        SquadCalibration calibration = service.calibrate(List.of(veteran, recent), REFERENCE_DAY);

        assertThat(calibration.windowMonths()).isEqualTo(4);
        assertThat(calibration.firstDay()).isEqualTo(REFERENCE_DAY.minusMonths(4));
        assertThat(calibration.players())
            .extracting(PlayerCalibration::covered)
            .containsExactly(true, true);
    }

    @Test
    @DisplayName("Hands a beginner the squad's median and never shrinks the window for them")
    void shouldGiveABeginnerTheSquadMedian() {
        Player light = player(1, "Alpha");
        Player heavy = player(2, "Bravo");
        Player beginner = player(3, "Charlie");

        when(playerMatchRepository.findEarliestMatchStartedAt(1L))
            .thenReturn(java.util.Optional.of(instant(NINE_MONTH_START.minusDays(1))));
        when(playerMatchRepository.findEarliestMatchStartedAt(2L))
            .thenReturn(java.util.Optional.of(instant(NINE_MONTH_START.minusDays(1))));
        when(playerMatchRepository.findEarliestMatchStartedAt(3L))
            .thenReturn(java.util.Optional.of(instant(REFERENCE_DAY.minusDays(3))));
        produces(light, LIGHT_TOTAL);
        produces(heavy, HEAVY_TOTAL);
        produces(beginner, 0);
        noMatches(light, heavy, beginner);

        SquadCalibration calibration = service.calibrate(List.of(light, heavy, beginner), REFERENCE_DAY);

        assertThat(calibration.windowMonths()).isEqualTo(9);
        assertThat(calibration.players())
            .extracting(PlayerCalibration::weeklyAverage)
            .containsExactly(700, 7_000, 3_850);
        assertThat(calibration.players().get(2).beginner()).isTrue();
        assertThat(calibration.reference()).isEqualTo(3_850);
    }

    @Test
    @DisplayName("Treats a player without a single match as a beginner")
    void shouldTreatAPlayerWithoutHistoryAsABeginner() {
        Player known = player(1, "Alpha");
        Player unknown = player(2, "Bravo");

        when(playerMatchRepository.findEarliestMatchStartedAt(1L))
            .thenReturn(java.util.Optional.of(instant(NINE_MONTH_START.minusDays(1))));
        when(playerMatchRepository.findEarliestMatchStartedAt(2L)).thenReturn(java.util.Optional.empty());
        produces(known, HEAVY_TOTAL);
        produces(unknown, 0);
        noMatches(known, unknown);

        SquadCalibration calibration = service.calibrate(List.of(known, unknown), REFERENCE_DAY);

        assertThat(calibration.players().get(1).beginner()).isTrue();
        assertThat(calibration.players().get(1).earliestMatchDay()).isNull();
        assertThat(calibration.players().get(1).weeklyAverage()).isEqualTo(7_000);
    }

    @Test
    @DisplayName("Bounds the volume factor so a quiet or a heavy squad still gets readable targets")
    void shouldBoundTheVolumeFactor() {
        Player quiet = player(1, "Alpha");

        covered(quiet);
        produces(quiet, 0);
        noMatches(quiet);

        SquadCalibration calibration = service.calibrate(List.of(quiet), REFERENCE_DAY);

        assertThat(calibration.scaling().volumeFactor()).isEqualByComparingTo(new BigDecimal("0.40"));
    }

    @Test
    @DisplayName("Measures the squad's talent as a median of medians, never as a volume")
    void shouldMeasureTalentAnchors() {
        Player steady = player(1, "Alpha");
        Player spiky = player(2, "Bravo");

        covered(steady, spiky);
        produces(steady, LIGHT_TOTAL);
        produces(spiky, LIGHT_TOTAL);
        when(playerMatchRepository.findForChallengePeriod(eq(1L), any(), any()))
            .thenReturn(List.of(competitive(10), competitive(20)));
        when(playerMatchRepository.findForChallengePeriod(eq(2L), any(), any()))
            .thenReturn(List.of(competitive(30), competitive(30), competitive(30)));

        SquadCalibration calibration = service.calibrate(List.of(steady, spiky), REFERENCE_DAY);

        // Steady's median is 15, spiky's is 30: the squad's is their average, 22.5, rounded to 23.
        assertThat(calibration.scaling().anchor(SkillAnchor.LONG_KILLS))
            .contains(BigDecimal.valueOf(23));
    }

    @Test
    @DisplayName("Refuses to calibrate an empty roster")
    void shouldRefuseAnEmptyRoster() {
        assertThatThrownBy(() -> service.calibrate(List.of(), REFERENCE_DAY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty roster");
    }

    /**
     * Declares that every listed player's history reaches past the nine-month window.
     *
     * @param players players to cover
     */
    private void covered(Player... players) {
        for (Player player : players) {
            when(playerMatchRepository.findEarliestMatchStartedAt(player.getId()))
                .thenReturn(java.util.Optional.of(instant(NINE_MONTH_START.minusDays(1))));
        }
    }

    /**
     * Declares what one player produced over the nine-month window, all on its first day.
     *
     * @param player player to stub
     * @param total  damage produced over the window
     */
    private void produces(Player player, int total) {
        when(dailyOutputReader.readPlayer(player.getId(), NINE_MONTH_START, REFERENCE_DAY))
            .thenReturn(output(player.getId(), NINE_MONTH_START, total));
    }

    /**
     * Declares what one player produced, whatever window the service settles on.
     *
     * @param player player to stub
     * @param total  damage produced over the window
     */
    private void producesAnyWindow(Player player, int total) {
        lenient().when(dailyOutputReader.readPlayer(eq(player.getId()), any(), any()))
            .thenReturn(output(player.getId(), NINE_MONTH_START, total));
    }

    /**
     * Declares that the listed players have no per-match statistics to anchor on.
     *
     * @param players players to stub
     */
    private void noMatches(Player... players) {
        for (Player player : players) {
            lenient().when(playerMatchRepository.findForChallengePeriod(eq(player.getId()), any(), any()))
                .thenReturn(List.of());
        }
    }

    /**
     * Builds a reading holding one player's whole window on one day.
     *
     * @param playerId player the output belongs to
     * @param day      day the damage lands on
     * @param damage   damage produced
     * @return the reading
     */
    private DailyOutput output(long playerId, LocalDate day, int damage) {
        Map<LocalDate, Map<Long, PlayerDayOutput>> byDay = new HashMap<>();
        byDay.put(day, Map.of(playerId, new PlayerDayOutput(damage, damage / 2, damage - damage / 2, 1, 0, 1, 0)));

        return new DailyOutput(byDay, Map.of(), List.of());
    }

    /**
     * Builds one competitive match with a given number of kills.
     *
     * @param kills kills scored
     * @return the player match
     */
    private PlayerMatch competitive(int kills) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(GameMode.COMPETITIVE);
        match.setStartedAt(instant(REFERENCE_DAY.minusDays(1)));

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(10);

        return playerMatch;
    }

    /**
     * Builds a tracked player.
     *
     * @param id       identifier
     * @param gameName Riot name
     * @return the player
     */
    private Player player(long id, String gameName) {
        Player player = new Player();
        player.setId(id);
        player.setGameName(gameName);
        player.setTagLine("EUW");

        return player;
    }

    /**
     * Returns the instant one day starts at, in the project's zone.
     *
     * @param day calendar day
     * @return the day's first instant
     */
    private Instant instant(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
