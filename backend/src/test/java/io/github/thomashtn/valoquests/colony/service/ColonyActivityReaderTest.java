package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDayActivity;
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
import io.github.thomashtn.valoquests.scoring.service.DailyMatchDamageReader;
import io.github.thomashtn.valoquests.scoring.service.MatchDamageCalculator;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the two readings the colony takes off a stretch of the calendar.
 *
 * <p>They are deliberately not the same number: what was brought home goes through the scoring
 * ruleset's daily diminishing returns, who turned up is read on raw damage before them.
 */
class ColonyActivityReaderTest {

    /** Monday the fixture week starts on. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 1);

    /** Damage a competitive win is priced at. */
    private static final int COMPETITIVE_WIN = 500;

    /** Damage a deathmatch is priced at short of its forty-kill victory. */
    private static final int DEATHMATCH = 100;

    /** Player match repository dependency. */
    private PlayerMatchRepository playerMatchRepository;

    /** Every fixture match, whoever played it, as the single range query would return them. */
    private List<PlayerMatch> storedMatches;

    /** Next identifier handed to a fixture match. */
    private long nextMatchId = 1L;

    /** Reader under test. */
    private ColonyActivityReader reader;

    /** Creates the collaborators before each test, wiring the real scoring pipeline. */
    @BeforeEach
    void setUp() {
        playerMatchRepository = mock(PlayerMatchRepository.class);
        storedMatches = new ArrayList<>();

        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:15:00Z"), ZoneOffset.UTC);
        WeekCalendar weekCalendar = new WeekCalendar(clock, ZoneOffset.UTC);

        // The real calculator and resolver, not mocks: the point of this reader is that the colony and
        // the ranking price a match identically, which a stubbed resolver could not demonstrate.
        MatchDamageCalculator damageCalculator =
            new MatchDamageCalculator(new MatchEligibility(), new MatchOutcomeResolver());
        WeeklyMatchDamageResolver damageResolver =
            new WeeklyMatchDamageResolver(damageCalculator, weekCalendar);

        // The range query, faked rather than stubbed flat: the reader now groups a single flat list
        // by player and week itself, so a stub returning everything unconditionally would not exercise
        // the grouping the diminishing returns depend on. The status is honoured for the same reason —
        // the reader asking for one is the whole of how a deactivated player leaves the colony.
        lenient().when(playerMatchRepository.findAllForPeriod(any(), any(), any()))
            .thenAnswer(invocation -> {
                Collection<PlayerStatus> statuses = invocation.getArgument(0);
                Instant from = invocation.getArgument(1);
                Instant to = invocation.getArgument(2);

                return storedMatches.stream()
                    .filter(match -> statuses.contains(match.getPlayer().getStatus()))
                    .filter(match -> !match.getMatch().getStartedAt().isBefore(from)
                        && match.getMatch().getStartedAt().isBefore(to))
                    .sorted(Comparator
                        .comparing((PlayerMatch match) -> match.getMatch().getStartedAt())
                        .thenComparing(PlayerMatch::getId))
                    .toList();
            });

        reader = new ColonyActivityReader(
            new DailyMatchDamageReader(
                playerMatchRepository,
                damageResolver,
                damageCalculator,
                new DefaultScoringRuleset(),
                weekCalendar
            ),
            new DefaultColonyRuleset(new DefaultScoringRuleset())
        );
    }

    /**
     * Verifies that a day's damage is every player's contribution summed.
     */
    @Test
    void shouldSumEveryPlayersDamageOnADay() {
        Player alpha = givenPlayer(1L);
        Player bravo = givenPlayer(2L);

        givenMatches(alpha, matches(MONDAY, 2));
        givenMatches(bravo, matches(MONDAY, 1));

        Map<LocalDate, ColonyDayActivity> activity = reader.readActivity(MONDAY, MONDAY);

        assertThat(activity.get(MONDAY).matchDamage()).isEqualTo(3 * COMPETITIVE_WIN);
        assertThat(activity.get(MONDAY).presencePlayerCount()).isEqualTo(2);
    }

    /**
     * Verifies that a player off the roster feeds neither the harvest nor the turnout.
     *
     * <p>The regression this reader was built the other way round for. A deactivated player used to
     * keep bringing food in while appearing on none of the gauges the roster draws, so a day's harvest
     * could have no author anywhere in the interface — and an operator who had deliberately taken
     * somebody off the roster went on being fed by them.
     */
    @Test
    void shouldIgnoreAPlayerOffTheRoster() {
        Player rostered = givenPlayer(1L);
        Player deactivated = givenPlayer(2L, PlayerStatus.INACTIVE);

        givenMatches(rostered, matches(MONDAY, 1));
        givenMatches(deactivated, matches(MONDAY, 4));

        Map<LocalDate, ColonyDayActivity> activity = reader.readActivity(MONDAY, MONDAY);

        assertThat(activity.get(MONDAY).matchDamage()).isEqualTo(COMPETITIVE_WIN);
        assertThat(activity.get(MONDAY).presencePlayerCount()).isEqualTo(1);
        assertThat(reader.readRawDamageByPlayer(MONDAY)).containsOnlyKeys(1L);
    }

    /**
     * Verifies that a day only the deactivated played reads as a day nobody played.
     */
    @Test
    void shouldReportNoActivityOnADayOnlyAPlayerOffTheRosterPlayed() {
        Player deactivated = givenPlayer(2L, PlayerStatus.ARCHIVED);
        givenMatches(deactivated, matches(MONDAY, 6));

        assertThat(reader.readActivity(MONDAY, MONDAY)).isEmpty();
    }

    /**
     * Verifies that the daily diminishing returns are inherited rather than recomputed.
     *
     * <p>Ten games in a day: five at full damage, four at half, one at a quarter. That reduction is the
     * whole reason the colony needs no anti-farming rule of its own.
     */
    @Test
    void shouldInheritTheDailyDiminishingReturns() {
        Player alpha = givenPlayer(1L);
        givenMatches(alpha, matches(MONDAY, 10));

        int expected = 5 * COMPETITIVE_WIN
            + 4 * (COMPETITIVE_WIN / 2)
            + COMPETITIVE_WIN / 4;

        assertThat(reader.readActivity(MONDAY, MONDAY).get(MONDAY).matchDamage())
            .isEqualTo(expected);
    }

    /**
     * Verifies that playing more does not move the turnout count.
     *
     * <p>The two readings are strictly independent: one counts what was played, the other counts who
     * showed up. It is also why the turnout is read on raw damage — read on the reduced figure, a player
     * stringing twelve games together could watch their own turnout drop by playing more.
     */
    @Test
    void shouldCountAPlayerOnceHoweverManyMatchesTheyPlayed() {
        Player alpha = givenPlayer(1L);
        givenMatches(alpha, matches(MONDAY, 12));

        assertThat(reader.readActivity(MONDAY, MONDAY).get(MONDAY).presencePlayerCount())
            .isEqualTo(1);
    }

    /**
     * Verifies a day under the threshold brings its food in and still does not count towards turnout.
     *
     * <p>Two deathmatches come to 200 raw damage. Without a threshold everybody would fire up a
     * two-minute deathmatch to reach the full multiplier.
     */
    @Test
    void shouldLeaveADayUnderTheThresholdOutOfTheTurnout() {
        Player alpha = givenPlayer(1L);
        givenMatches(alpha, deathmatches(MONDAY, 2));

        ColonyDayActivity activity = reader.readActivity(MONDAY, MONDAY).get(MONDAY);

        assertThat(activity.matchDamage()).isPositive();
        assertThat(activity.presencePlayerCount()).isZero();
    }

    /**
     * Verifies three deathmatches do clear the threshold, at exactly 300, which is the equivalence it was
     * picked for: one competitive game, or three deathmatches.
     */
    @Test
    void shouldCountThreeDeathmatchesAsAnEvening() {
        Player alpha = givenPlayer(1L);
        givenMatches(alpha, deathmatches(MONDAY, 3));

        assertThat(reader.readActivity(MONDAY, MONDAY).get(MONDAY).presencePlayerCount())
            .isEqualTo(1);
    }

    /**
     * Verifies the per-player reading the turnout readout is drawn from, raw damage and all.
     */
    @Test
    void shouldReportWhatEachPlayerBroughtToADay() {
        Player alpha = givenPlayer(1L);
        Player bravo = givenPlayer(2L);

        givenMatches(alpha, matches(MONDAY, 2));
        givenMatches(bravo, deathmatches(MONDAY, 1));

        assertThat(reader.readRawDamageByPlayer(MONDAY))
            .containsEntry(1L, 2 * COMPETITIVE_WIN)
            .containsEntry(2L, DEATHMATCH);
    }

    /**
     * Verifies a day nobody played reports nobody, rather than an entry at zero.
     */
    @Test
    void shouldReportNobodyOnADayNobodyPlayed() {
        givenPlayer(1L);

        assertThat(reader.readRawDamageByPlayer(MONDAY)).isEmpty();
        assertThat(reader.presenceCount(Map.of())).isZero();
    }

    /**
     * Verifies that days are separated, so a week's matches land on the day they were played.
     */
    @Test
    void shouldSplitAWeeksMatchesAcrossTheDaysTheyWerePlayedOn() {
        Player alpha = givenPlayer(1L);

        List<PlayerMatch> week = new ArrayList<>();
        week.addAll(matches(MONDAY, 2));
        week.addAll(matches(MONDAY.plusDays(3), 1));
        givenMatches(alpha, week);

        Map<LocalDate, ColonyDayActivity> activity = reader.readActivity(MONDAY, MONDAY.plusDays(6));

        assertThat(activity.get(MONDAY).matchDamage()).isEqualTo(2 * COMPETITIVE_WIN);
        assertThat(activity.get(MONDAY.plusDays(3)).matchDamage()).isEqualTo(COMPETITIVE_WIN);
        assertThat(activity).doesNotContainKey(MONDAY.plusDays(1));
    }

    /**
     * Verifies that days outside the requested range are dropped, even when their week was walked.
     */
    @Test
    void shouldDropDaysOutsideTheRequestedRange() {
        Player alpha = givenPlayer(1L);

        List<PlayerMatch> week = new ArrayList<>();
        week.addAll(matches(MONDAY, 1));
        week.addAll(matches(MONDAY.plusDays(5), 1));
        givenMatches(alpha, week);

        Map<LocalDate, ColonyDayActivity> activity = reader.readActivity(MONDAY, MONDAY.plusDays(2));

        assertThat(activity).containsOnlyKeys(MONDAY);
    }

    /**
     * Registers one tracked player.
     *
     * @param id internal player identifier
     * @return the player
     */
    private Player givenPlayer(long id) {
        return givenPlayer(id, Player.COMPETITIVE_STATUS);
    }

    /**
     * Creates a player holding a given status.
     *
     * @param id     internal identifier
     * @param status status the player holds
     * @return the player
     */
    private Player givenPlayer(long id, PlayerStatus status) {
        Player player = new Player();
        player.setId(id);
        player.setGameName("Player" + id);
        player.setTagLine("EUW");
        player.setStatus(status);

        return player;
    }

    /**
     * Registers one player's matches.
     *
     * @param player  player who played them
     * @param matches their matches
     */
    private void givenMatches(Player player, List<PlayerMatch> matches) {
        matches.forEach(match -> match.setPlayer(player));
        storedMatches.addAll(matches);
    }

    /**
     * Builds a number of competitive wins played on one day.
     *
     * @param day   day they were played on
     * @param count how many
     * @return the matches
     */
    private List<PlayerMatch> matches(LocalDate day, int count) {
        return matches(day, count, GameMode.COMPETITIVE);
    }

    /**
     * Builds a number of deathmatch wins played on one day, the cheapest thing a player can queue.
     *
     * @param day   day they were played on
     * @param count how many
     * @return the matches
     */
    private List<PlayerMatch> deathmatches(LocalDate day, int count) {
        return matches(day, count, GameMode.DEATHMATCH);
    }

    /**
     * Builds a number of wins in one mode, played on one day.
     *
     * @param day      day they were played on
     * @param count    how many
     * @param gameMode mode they were played in
     * @return the matches
     */
    private List<PlayerMatch> matches(LocalDate day, int count, GameMode gameMode) {
        List<PlayerMatch> matches = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            ValorantMatch valorantMatch = new ValorantMatch();
            valorantMatch.setStartedAt(day.atTime(18, index % 24).toInstant(ZoneOffset.UTC));
            valorantMatch.setGameMode(gameMode);

            PlayerMatch playerMatch = new PlayerMatch();
            playerMatch.setId(nextMatchId++);
            playerMatch.setMatch(valorantMatch);
            playerMatch.setResult(MatchResult.WIN);
            playerMatch.setRoundsPlayed(24);
            playerMatch.setScore(5_000);

            matches.add(playerMatch);
        }

        return matches;
    }
}
