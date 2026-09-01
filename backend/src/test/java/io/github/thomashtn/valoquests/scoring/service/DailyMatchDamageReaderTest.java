package io.github.thomashtn.valoquests.scoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

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
import io.github.thomashtn.valoquests.scoring.model.DailyMatchDamage;
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
 * Tests the reader both pillars price a day through.
 *
 * <p>The whole point of it living in {@code scoring/} is that one evening is worth the same figure to
 * the colony and to the leaderboard's day scope, so the real calculator and resolver are wired in
 * rather than stubbed.
 */
@DisplayName("Daily match damage")
class DailyMatchDamageReaderTest {

    /** Monday the fixture week starts on. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 1);

    /** Damage a competitive win is priced at, before diminishing returns. */
    private static final int COMPETITIVE_WIN = 500;

    /** Every fixture match, whoever played it, as the single range query would return them. */
    private List<PlayerMatch> storedMatches;

    /** Next identifier handed to a fixture match. */
    private long nextMatchId = 1L;

    /** Reader under test. */
    private DailyMatchDamageReader reader;

    /** Wires the real scoring pipeline behind a faked range query. */
    @BeforeEach
    void setUp() {
        PlayerMatchRepository playerMatchRepository = mock(PlayerMatchRepository.class);
        storedMatches = new ArrayList<>();

        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:15:00Z"), ZoneOffset.UTC);
        WeekCalendar weekCalendar = new WeekCalendar(clock, ZoneOffset.UTC);
        MatchDamageCalculator damageCalculator =
            new MatchDamageCalculator(new MatchEligibility(), new MatchOutcomeResolver());

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

        reader = new DailyMatchDamageReader(
            playerMatchRepository,
            new WeeklyMatchDamageResolver(damageCalculator, weekCalendar),
            damageCalculator,
            new DefaultScoringRuleset(),
            weekCalendar
        );
    }

    @Test
    @DisplayName("attributes a day's damage to the player who dealt it")
    void shouldAttributeADaysDamageToThePlayerWhoDealtIt() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), matches(MONDAY, 2));
        givenMatches(player(2L, PlayerStatus.ACTIVE), matches(MONDAY, 1));

        DailyMatchDamage damage = reader.read(everyone(), MONDAY, MONDAY);

        assertThat(damage.weightedDamageOn(MONDAY))
            .containsEntry(1L, 2 * COMPETITIVE_WIN)
            .containsEntry(2L, COMPETITIVE_WIN);
        assertThat(damage.weightedDamageByDay()).containsEntry(MONDAY, 3 * COMPETITIVE_WIN);
    }

    @Test
    @DisplayName("applies the daily diminishing returns per player rather than per squad")
    void shouldApplyTheDiminishingReturnsPerPlayer() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), matches(MONDAY, 10));
        givenMatches(player(2L, PlayerStatus.ACTIVE), matches(MONDAY, 1));

        DailyMatchDamage damage = reader.read(everyone(), MONDAY, MONDAY);

        int reduced = 5 * COMPETITIVE_WIN + 4 * (COMPETITIVE_WIN / 2) + COMPETITIVE_WIN / 4;
        assertThat(damage.weightedDamageOn(MONDAY))
            .containsEntry(1L, reduced)
            .containsEntry(2L, COMPETITIVE_WIN);
    }

    @Test
    @DisplayName("honours the statuses it is asked for, so each caller states its own roster")
    void shouldHonourTheStatusesItIsAskedFor() {
        givenMatches(player(1L, PlayerStatus.ACTIVE), matches(MONDAY, 1));
        givenMatches(player(2L, PlayerStatus.INACTIVE), matches(MONDAY, 1));

        assertThat(reader.read(Set.of(PlayerStatus.ACTIVE), MONDAY, MONDAY).weightedDamageOn(MONDAY))
            .containsOnlyKeys(1L);
        assertThat(reader.read(everyone(), MONDAY, MONDAY).weightedDamageOn(MONDAY))
            .containsOnlyKeys(1L, 2L);
    }

    @Test
    @DisplayName("separates two days of the same week, per player")
    void shouldSeparateTwoDaysOfTheSameWeek() {
        Player alpha = player(1L, PlayerStatus.ACTIVE);
        givenMatches(alpha, matches(MONDAY, 2));
        givenMatches(alpha, matches(MONDAY.plusDays(1), 1));

        DailyMatchDamage damage = reader.read(everyone(), MONDAY, MONDAY.plusDays(1));

        assertThat(damage.weightedDamageOn(MONDAY)).containsEntry(1L, 2 * COMPETITIVE_WIN);
        assertThat(damage.weightedDamageOn(MONDAY.plusDays(1))).containsEntry(1L, COMPETITIVE_WIN);
    }

    @Test
    @DisplayName("omits a day nobody played rather than reporting it at zero")
    void shouldOmitADayNobodyPlayed() {
        DailyMatchDamage damage = reader.read(everyone(), MONDAY, MONDAY);

        assertThat(damage.weightedDamageOn(MONDAY)).isEmpty();
        assertThat(damage.rawDamageOn(MONDAY)).isEmpty();
    }

    /**
     * Returns the statuses the day's board asks for.
     *
     * @return active and inactive players
     */
    private Set<PlayerStatus> everyone() {
        return Set.of(PlayerStatus.ACTIVE, PlayerStatus.INACTIVE);
    }

    /**
     * Creates a player holding a given status.
     *
     * @param id     internal identifier
     * @param status status the player holds
     * @return the player
     */
    private Player player(long id, PlayerStatus status) {
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
        List<PlayerMatch> matches = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            ValorantMatch valorantMatch = new ValorantMatch();
            valorantMatch.setStartedAt(day.atTime(18, index % 24).toInstant(ZoneOffset.UTC));
            valorantMatch.setGameMode(GameMode.COMPETITIVE);

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
