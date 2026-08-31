package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.SeasonQueryService;
import io.github.thomashtn.valoquests.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultPlayerQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultPlayerQueryServiceTest {

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
     * Mocked week calendar, never resolving a week bound since these tests never pass a weekStart.
     */
    @Mock
    private WeekCalendar weekCalendar;

    /**
     * Mocked season query service, resolving the season currently in progress.
     */
    @Mock
    private SeasonQueryService seasonQueryService;

    /**
     * Mocked resolver of the daily diminishing-returns ladder.
     */
    @Mock
    private WeeklyMatchDamageResolver weeklyMatchDamageResolver;

    /**
     * Mocked ruleset supplying that ladder.
     */
    @Mock
    private ScoringRuleset ruleset;

    /**
     * Service under test.
     */
    private DefaultPlayerQueryService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new DefaultPlayerQueryService(
            playerRepository,
            playerMatchRepository,
            weekCalendar,
            seasonQueryService,
            weeklyMatchDamageResolver,
            ruleset
        );

        // The profile carries today's standing on the diminishing-returns ladder, which every
        // `findById` therefore resolves. Lenient because the list-facing tests never reach it.
        lenient().when(weekCalendar.today()).thenReturn(LocalDate.of(2026, 8, 31));
        lenient().when(weekCalendar.zone()).thenReturn(ZoneOffset.UTC);
        lenient().when(weeklyMatchDamageResolver.dailyYield(any(), any(), any()))
            .thenReturn(new WeeklyMatchDamageResolver.DailyYield(0, 100, 6, 50));
    }

    /**
     * Verifies that the player list's statistics are scoped to the season currently in progress, as
     * resolved by {@link SeasonQueryService}, and to competitive matches.
     */
    @Test
    void shouldScopePlayerListStatisticsToTheCurrentSeasonAndCompetitiveMode() {
        when(seasonQueryService.resolveCurrentSeasonId()).thenReturn(5L);
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player(1L)));
        when(
            playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(
                1L,
                new PlayerMatchHistoryCriteria(
                    5L, null, null, null, GameMode.COMPETITIVE,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
                )
            )
        ).thenReturn(List.of(match(MatchResult.WIN, "Jett", "Ascent")));

        List<PlayerSummaryResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().matchesPlayed()).isEqualTo(1);
    }

    /**
     * Verifies that the player list falls back to every competitive match on record when no season
     * is known yet.
     */
    @Test
    void shouldComputePlayerListLifetimeAggregateWhenNoSeasonExistsYet() {
        when(seasonQueryService.resolveCurrentSeasonId()).thenReturn(null);
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(player(1L)));
        when(
            playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(
                1L,
                new PlayerMatchHistoryCriteria(
                    null, null, null, null, GameMode.COMPETITIVE,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
                )
            )
        ).thenReturn(List.of());

        List<PlayerSummaryResponse> result = service.findAll();

        assertThat(result.getFirst().matchesPlayed()).isEqualTo(0);
    }

    /**
     * Verifies that statistics are computed only from the matches the repository returns for the
     * requested season and game mode, forwarding both as parsed filters.
     */
    @Test
    void shouldScopeStatisticsToTheRequestedSeasonAndGameMode() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(
            playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(
                1L,
                new PlayerMatchHistoryCriteria(
                    5L, null, null, null, GameMode.COMPETITIVE,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
                )
            )
        ).thenReturn(List.of(match(MatchResult.WIN, "Jett", "Ascent")));

        PlayerDetailsResponse response = service.findById(1L, 5L, "competitive", null);

        assertThat(response.statistics().matchesPlayed()).isEqualTo(1);
        assertThat(response.statistics().wins()).isEqualTo(1);
        assertThat(response.statistics().losses()).isEqualTo(0);
    }

    /**
     * Verifies that a supplied weekStart is resolved through the week calendar and forwarded as the
     * matches' instant period, narrowing statistics to that calendar week.
     */
    @Test
    void shouldScopeStatisticsToTheRequestedWeek() {
        LocalDate weekStart = LocalDate.of(2026, 7, 27);
        Instant periodStart = Instant.parse("2026-07-27T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-08-03T00:00:00Z");

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(weekCalendar.startOf(weekStart)).thenReturn(periodStart);
        when(weekCalendar.endOf(weekStart)).thenReturn(periodEnd);
        when(
            playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(
                1L, new PlayerMatchHistoryCriteria(null, null, null, null, null, periodStart, periodEnd)
            )
        ).thenReturn(List.of(match(MatchResult.WIN, "Jett", "Ascent")));

        PlayerDetailsResponse response = service.findById(1L, null, null, weekStart);

        assertThat(response.statistics().matchesPlayed()).isEqualTo(1);
    }

    /**
     * Verifies that omitting both filters reproduces the unfiltered, lifetime aggregate - a
     * regression guard for callers that still request every season and every mode.
     */
    @Test
    void shouldComputeLifetimeAggregateWhenNoFilterIsSupplied() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(
            playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(
                1L,
                new PlayerMatchHistoryCriteria(
                    null, null, null, null, null,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                    PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
                )
            )
        ).thenReturn(List.of(
            match(MatchResult.WIN, "Jett", "Ascent"),
            match(MatchResult.LOSS, "Reyna", "Bind")
        ));

        PlayerDetailsResponse response = service.findById(1L, null, null, null);

        assertThat(response.statistics().matchesPlayed()).isEqualTo(2);
        assertThat(response.statistics().wins()).isEqualTo(1);
        assertThat(response.statistics().losses()).isEqualTo(1);
    }

    /**
     * Verifies that an unrecognized game mode is rejected before any match data is loaded.
     */
    @Test
    void shouldRejectAnUnknownGameMode() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));

        assertThatThrownBy(() -> service.findById(1L, null, "not-a-mode", null))
            .isInstanceOf(InvalidRequestException.class);
    }

    /**
     * Verifies that requesting an untracked player still fails fast regardless of filters.
     */
    @Test
    void shouldThrowWhenPlayerDoesNotExist() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L, null, null, null))
            .isInstanceOf(PlayerNotFoundException.class);
    }

    private Player player(long id) {
        Player player = new Player();
        player.setId(id);
        player.setGameName("Player");
        player.setTagLine("EUW");
        player.setDisplayName("Player");
        return player;
    }

    private PlayerMatch match(MatchResult result, String agentName, String mapName) {
        ValorantMatch valorantMatch = new ValorantMatch();
        valorantMatch.setMapName(mapName);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(valorantMatch);
        playerMatch.setAgentName(agentName);
        playerMatch.setResult(result);
        playerMatch.setKills(10);
        playerMatch.setDeaths(5);
        playerMatch.setAssists(3);
        playerMatch.setAcs(BigDecimal.valueOf(200));
        playerMatch.setAdr(BigDecimal.valueOf(150));
        return playerMatch;
    }
}
