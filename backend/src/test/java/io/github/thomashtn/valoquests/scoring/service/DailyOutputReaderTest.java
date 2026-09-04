package io.github.thomashtn.valoquests.scoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.DailyYield;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the reader everything prices a day through.
 *
 * <p>The whole point of it living in {@code scoring/} is that one evening is worth the same figure to
 * the campaign, to the ranking and to the match history, so the real calculator and ruleset are
 * wired in rather than stubbed.
 */
@DisplayName("Daily output")
class DailyOutputReaderTest {

    /** Monday the fixture week starts on. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 1);

    /** Value of a competitive win before any multiplier. */
    private static final int COMPETITIVE_WIN = 500;

    /** Value of a deathmatch win before any multiplier. */
    private static final int DEATHMATCH_WIN = 150;

    /** Every fixture match, whoever played it, as the range query would return them. */
    private List<PlayerMatch> storedMatches;

    /** Faked range query. */
    private PlayerMatchRepository playerMatchRepository;

    /** Next identifier handed to a fixture match. */
    private long nextMatchId = 1L;

    /** Reader under test. */
    private DailyOutputReader reader;

    /** Wires the real scoring pipeline behind a faked range query. */
    @BeforeEach
    void setUp() {
        playerMatchRepository = mock(PlayerMatchRepository.class);
        storedMatches = new ArrayList<>();

        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:15:00Z"), ZoneOffset.UTC);
        WeekCalendar weekCalendar = new WeekCalendar(clock, ZoneOffset.UTC);
        MatchDamageCalculator damageCalculator =
            new MatchDamageCalculator(new MatchEligibility(), new MatchOutcomeResolver());

        lenient().when(playerMatchRepository.findAllForPeriod(any(), any(), any()))
            .thenAnswer(invocation -> {
                Collection<PlayerStatus> statuses = invocation.getArgument(0);
                return stored(invocation.getArgument(1), invocation.getArgument(2)).stream()
                    .filter(match -> statuses.contains(match.getPlayer().getStatus()))
                    .toList();
            });
        lenient().when(playerMatchRepository.findForChallengePeriod(anyLong(), any(), any()))
            .thenAnswer(invocation -> {
                Long playerId = invocation.getArgument(0);
                return stored(invocation.getArgument(1), invocation.getArgument(2)).stream()
                    .filter(match -> match.getPlayer().getId().equals(playerId))
                    .toList();
            });

        reader = new DailyOutputReader(
            playerMatchRepository, damageCalculator, new DefaultScoringRuleset(), weekCalendar
        );
    }

    @Test
    @DisplayName("attributes a day's output to the player who produced it")
    void shouldAttributeADaysOutputToThePlayerWhoProducedIt() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 2));
        givenMatches(player(2L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 1));

        DailyOutput output = reader.read(everyone(), MONDAY, MONDAY);

        assertThat(output.of(1L, MONDAY).damage()).isEqualTo(2 * COMPETITIVE_WIN);
        assertThat(output.of(2L, MONDAY).damage()).isEqualTo(COMPETITIVE_WIN);
        assertThat(output.of(3L, MONDAY)).isEqualTo(PlayerDayOutput.NONE);
        assertThat(output.on(MONDAY.plusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("splits a match into food and components by its mode")
    void shouldSplitAMatchIntoFoodAndComponentsByItsMode() {
        Player player = player(1L, PlayerStatus.ACTIVE);
        givenMatches(player, competitiveWins(MONDAY, 1));
        givenMatches(player, deathmatchWins(MONDAY, 1));

        PlayerDayOutput day = reader.read(everyone(), MONDAY, MONDAY).of(1L, MONDAY);

        assertThat(day.damage()).isEqualTo(COMPETITIVE_WIN + DEATHMATCH_WIN);
        assertThat(day.food()).isEqualTo(150 + 105);
        assertThat(day.components()).isEqualTo(350 + 45);
        assertThat(day.matchCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("applies the daily diminishing returns per player, best games first")
    void shouldApplyTheDiminishingReturnsPerPlayer() {
        Player grinder = player(1L, PlayerStatus.ACTIVE);
        givenMatches(grinder, deathmatchWins(MONDAY, 5));
        givenMatches(grinder, competitiveWins(MONDAY, 5));
        givenMatches(player(2L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 1));

        DailyOutput output = reader.read(everyone(), MONDAY, MONDAY);

        // The five competitive games keep full value whatever order they were played in; the tenth
        // game is 150 × 0.25 = 37.5, rounded half up.
        int reduced = 5 * COMPETITIVE_WIN + 4 * (DEATHMATCH_WIN / 2) + 38;
        assertThat(output.of(1L, MONDAY).damage()).isEqualTo(reduced);
        assertThat(output.of(1L, MONDAY).reducedMatchCount()).isEqualTo(5);
        assertThat(output.of(2L, MONDAY).damage()).isEqualTo(COMPETITIVE_WIN);
        assertThat(output.of(2L, MONDAY).reducedMatchCount()).isZero();
    }

    @Test
    @DisplayName("grows the streak over consecutive played days and resets it on a gap")
    void shouldGrowTheStreakOverConsecutiveDaysAndResetItOnAGap() {
        Player player = player(1L, PlayerStatus.ACTIVE);
        for (int offset = 0; offset < 3; offset++) {
            givenMatches(player, competitiveWins(MONDAY.plusDays(offset), 1));
        }
        givenMatches(player, competitiveWins(MONDAY.plusDays(4), 1));

        DailyOutput output = reader.read(everyone(), MONDAY, MONDAY.plusDays(6));

        assertThat(output.of(1L, MONDAY).streakDays()).isEqualTo(1);
        assertThat(output.of(1L, MONDAY).streakBonusPercent()).isZero();
        assertThat(output.of(1L, MONDAY.plusDays(1)).streakDays()).isEqualTo(2);
        assertThat(output.of(1L, MONDAY.plusDays(1)).damage()).isEqualTo(510);
        assertThat(output.of(1L, MONDAY.plusDays(2)).streakDays()).isEqualTo(3);
        assertThat(output.of(1L, MONDAY.plusDays(2)).damage()).isEqualTo(520);
        assertThat(output.of(1L, MONDAY.plusDays(4)).streakDays()).isEqualTo(1);
        assertThat(output.streakEndingOn(1L, MONDAY.plusDays(2))).isEqualTo(3);
        assertThat(output.streakEndingOn(1L, MONDAY.plusDays(3))).isZero();
    }

    @Test
    @DisplayName("counts the streak from before the range, so the first day is not always day one")
    void shouldCountTheStreakFromBeforeTheRange() {
        Player player = player(1L, PlayerStatus.ACTIVE);
        for (int offset = 1; offset <= 6; offset++) {
            givenMatches(player, competitiveWins(MONDAY.minusDays(offset), 1));
        }
        givenMatches(player, competitiveWins(MONDAY, 1));

        DailyOutput output = reader.read(everyone(), MONDAY, MONDAY);

        assertThat(output.of(1L, MONDAY).streakDays()).isEqualTo(7);
        assertThat(output.of(1L, MONDAY).streakBonusPercent()).isEqualTo(10);
        assertThat(output.of(1L, MONDAY).damage()).isEqualTo(550);
        assertThat(output.streakEndingOn(1L, MONDAY.minusDays(1))).isEqualTo(6);
        assertThat(output.on(MONDAY.minusDays(1))).as("days before the range are not reported").isEmpty();
        verify(playerMatchRepository).findAllForPeriod(
            everyone(),
            MONDAY.minusDays(DailyOutputReader.STREAK_LOOKBACK_DAYS).atStartOfDay(ZoneOffset.UTC).toInstant(),
            MONDAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        );
    }

    @Test
    @DisplayName("rounds once, at the end, with both multipliers applied")
    void shouldRoundOnceWithBothMultipliersApplied() {
        Player player = player(1L, PlayerStatus.ACTIVE);
        givenMatches(player, competitiveWins(MONDAY.minusDays(1), 1));
        givenMatches(player, competitiveWins(MONDAY, 5));
        givenMatches(player, deathmatchWins(MONDAY, 1));

        List<ValuedMatch> valued = reader.read(everyone(), MONDAY, MONDAY).valuedMatches();

        ValuedMatch sixth = valued.stream()
            .filter(match -> match.coefficientPercent() == 50)
            .findFirst()
            .orElseThrow();
        // 150 × 0.5 × 1.02 = 76.5, rounded once to 77; then split 70/30 into 54 + 23.
        assertThat(sixth.damage()).isEqualTo(77);
        assertThat(sixth.food()).isEqualTo(54);
        assertThat(sixth.components()).isEqualTo(23);
        assertThat(sixth.streakBonusPercent()).isEqualTo(2);
    }

    @Test
    @DisplayName("ignores a match that is not valued: no rank, no day, no streak")
    void shouldIgnoreAMatchThatIsNotValued() {
        Player player = player(1L, PlayerStatus.ACTIVE);
        givenMatches(player, competitiveWins(MONDAY, 1));
        List<PlayerMatch> swiftplay = competitiveWins(MONDAY.plusDays(1), 1);
        swiftplay.getFirst().getMatch().setGameMode(GameMode.SWIFTPLAY);
        givenMatches(player, swiftplay);
        givenMatches(player, competitiveWins(MONDAY.plusDays(2), 1));

        DailyOutput output = reader.read(everyone(), MONDAY, MONDAY.plusDays(2));

        assertThat(output.on(MONDAY.plusDays(1))).isEmpty();
        assertThat(output.of(1L, MONDAY.plusDays(2)).streakDays()).isEqualTo(1);
        assertThat(output.valuedMatches()).hasSize(2);
    }

    @Test
    @DisplayName("lists valued matches in squad-wide chronological order")
    void shouldListValuedMatchesInSquadWideChronologicalOrder() {
        givenMatches(player(2L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 2));
        givenMatches(player(1L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 3));

        List<ValuedMatch> valued = reader.read(everyone(), MONDAY, MONDAY).valuedMatches();

        assertThat(valued).hasSize(5);
        assertThat(valued).isSortedAccordingTo(Comparator.comparing(ValuedMatch::startedAt));
        assertThat(valued.getFirst().playerId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("honours the statuses it is asked for, so each caller states its own roster")
    void shouldHonourTheStatusesItIsAskedFor() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 1));
        givenMatches(player(2L, PlayerStatus.INACTIVE), competitiveWins(MONDAY, 1));

        assertThat(reader.read(Set.of(PlayerStatus.ACTIVE), MONDAY, MONDAY).on(MONDAY)).containsOnlyKeys(1L);
        assertThat(reader.read(everyone(), MONDAY, MONDAY).on(MONDAY)).containsOnlyKeys(1L, 2L);
    }

    @Test
    @DisplayName("reads one player whatever their status")
    void shouldReadOnePlayerWhateverTheirStatus() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 1));
        givenMatches(player(2L, PlayerStatus.ARCHIVED), competitiveWins(MONDAY, 2));

        DailyOutput output = reader.readPlayer(2L, MONDAY, MONDAY);

        assertThat(output.on(MONDAY)).containsOnlyKeys(2L);
        assertThat(output.of(2L, MONDAY).damage()).isEqualTo(2 * COMPETITIVE_WIN);
    }

    @Test
    @DisplayName("says what the next match of the day is worth and where the ladder drops next")
    void shouldReportTheDailyYield() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 5));

        assertThat(reader.dailyYield(1L, MONDAY)).isEqualTo(new DailyYield(5, 50, 10, 25));
        assertThat(reader.dailyYield(2L, MONDAY)).isEqualTo(new DailyYield(0, 100, 6, 50));
    }

    @Test
    @DisplayName("stops probing the ladder once it has reached its floor")
    void shouldStopProbingTheLadderAtItsFloor() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), competitiveWins(MONDAY, 12));

        assertThat(reader.dailyYield(1L, MONDAY)).isEqualTo(new DailyYield(12, 25, null, null));
    }

    private List<PlayerMatch> stored(Instant from, Instant to) {
        return storedMatches.stream()
            .filter(match -> !match.getMatch().getStartedAt().isBefore(from)
                && match.getMatch().getStartedAt().isBefore(to))
            .sorted(Comparator
                .comparing((PlayerMatch match) -> match.getMatch().getStartedAt())
                .thenComparing(PlayerMatch::getId))
            .toList();
    }

    private static Set<PlayerStatus> everyone() {
        return Set.of(PlayerStatus.ACTIVE, PlayerStatus.INACTIVE);
    }

    private static Player player(long id, PlayerStatus status) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName("player-" + id);
        player.setStatus(status);
        return player;
    }

    private void givenMatches(Player player, List<PlayerMatch> matches) {
        matches.forEach(match -> match.setPlayer(player));
        storedMatches.addAll(matches);
    }

    private List<PlayerMatch> competitiveWins(LocalDate day, int count) {
        return matches(day, count, GameMode.COMPETITIVE, 18);
    }

    private List<PlayerMatch> deathmatchWins(LocalDate day, int count) {
        return matches(day, count, GameMode.DEATHMATCH, 12);
    }

    /**
     * Builds valued wins on one day, one per hour from the given hour, so chronology stays unique.
     */
    private List<PlayerMatch> matches(LocalDate day, int count, GameMode gameMode, int firstHour) {
        List<PlayerMatch> matches = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ValorantMatch valorantMatch = new ValorantMatch();
            valorantMatch.setStartedAt(day.atTime(firstHour % 24, index).toInstant(ZoneOffset.UTC));
            valorantMatch.setGameMode(gameMode);

            PlayerMatch playerMatch = new PlayerMatch();
            playerMatch.setId(nextMatchId++);
            playerMatch.setMatch(valorantMatch);
            playerMatch.setResult(MatchResult.WIN);
            playerMatch.setRoundsPlayed(gameMode == GameMode.DEATHMATCH ? 1 : 24);
            playerMatch.setScore(5_000);
            playerMatch.setKills(gameMode == GameMode.DEATHMATCH ? 40 : 20);
            matches.add(playerMatch);
        }
        return matches;
    }
}
