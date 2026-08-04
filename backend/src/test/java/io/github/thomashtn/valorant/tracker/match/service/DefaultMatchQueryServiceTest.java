package io.github.thomashtn.valorant.tracker.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.match.dto.MatchResponse;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchHistoryFilter;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.shared.exception.InvalidRequestException;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
 * Unit tests for {@link DefaultMatchQueryService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Match history queries")
class DefaultMatchQueryServiceTest {

    /**
     * Identifier of the tracked player used by every test.
     */
    private static final long PLAYER_ID = 1L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMatchRepository playerMatchRepository;

    @Mock
    private WeekCalendar weekCalendar;

    private DefaultMatchQueryService service;

    @BeforeEach
    void setUp() {
        service = new DefaultMatchQueryService(playerRepository, playerMatchRepository, weekCalendar);
    }

    /**
     * Makes the player exist and the repository return the supplied matches.
     */
    private void given(List<PlayerMatch> matches) {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(playerMatchRepository.findHistory(
            any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(matches, PageRequest.of(0, 10), matches.size()));
    }

    @Test
    @DisplayName("passes trimmed filters through and leaves blank ones unset")
    void shouldPassTrimmedFiltersThroughAndLeaveBlankOnesUnset() {
        given(List.of());

        service.findByPlayer(PLAYER_ID, 0, 10, new MatchHistoryFilter(
            7L, "  Ascent  ", "   ", "win", "competitive", null
        ));

        verify(playerMatchRepository).findHistory(
            eq(PLAYER_ID),
            eq(new PlayerMatchHistoryCriteria(
                7L, "Ascent", null, MatchResult.WIN, GameMode.COMPETITIVE,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            )),
            any(Pageable.class)
        );
    }

    @Test
    @DisplayName("treats an absent filter as no restriction rather than as a missing value")
    void shouldTreatAnAbsentFilterAsNoRestriction() {
        given(List.of());

        service.findByPlayer(PLAYER_ID, 0, 10, MatchHistoryFilter.NONE);

        verify(playerMatchRepository).findHistory(
            eq(PLAYER_ID),
            eq(new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            )),
            any(Pageable.class)
        );
    }

    @Test
    @DisplayName("resolves a supplied weekStart through the week calendar and forwards it as a period")
    void shouldResolveWeekStartThroughTheWeekCalendar() {
        given(List.of());

        LocalDate weekStart = LocalDate.of(2026, 7, 27);
        Instant periodStart = Instant.parse("2026-07-27T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-08-03T00:00:00Z");
        when(weekCalendar.startOf(weekStart)).thenReturn(periodStart);
        when(weekCalendar.endOf(weekStart)).thenReturn(periodEnd);

        service.findByPlayer(
            PLAYER_ID, 0, 10, new MatchHistoryFilter(null, null, null, null, null, weekStart)
        );

        verify(playerMatchRepository).findHistory(
            eq(PLAYER_ID),
            eq(new PlayerMatchHistoryCriteria(null, null, null, null, null, periodStart, periodEnd)),
            any(Pageable.class)
        );
    }

    @Test
    @DisplayName("derives KDA and headshot percentage from the stored counters")
    void shouldDeriveKdaAndHeadshotPercentage() {
        given(List.of(match(20, 8, 4, 30, 60, 10, "Red")));

        MatchResponse response = service.findByPlayer(
            PLAYER_ID, 0, 10, MatchHistoryFilter.NONE
        ).content().getFirst();

        // (20 kills + 4 assists) / 8 deaths
        assertThat(response.kda()).isEqualByComparingTo("3.00");
        // 30 headshots out of 100 shots
        assertThat(response.headshotPercentage()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("treats a deathless match as one death rather than dividing by zero")
    void shouldTreatADeathlessMatchAsOneDeath() {
        given(List.of(match(15, 0, 0, 5, 5, 0, "Red")));

        MatchResponse response = service.findByPlayer(
            PLAYER_ID, 0, 10, MatchHistoryFilter.NONE
        ).content().getFirst();

        assertThat(response.kda()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("reports a zero headshot percentage when no shot was recorded")
    void shouldReportZeroHeadshotPercentageWhenNoShotWasRecorded() {
        given(List.of(match(0, 1, 0, 0, 0, 0, "Red")));

        MatchResponse response = service.findByPlayer(
            PLAYER_ID, 0, 10, MatchHistoryFilter.NONE
        ).content().getFirst();

        assertThat(response.headshotPercentage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("orients the scoreline around the player's own team")
    void shouldOrientTheScorelineAroundThePlayersOwnTeam() {
        given(List.of(match(10, 10, 0, 0, 0, 0, "Blue")));

        MatchResponse response = service.findByPlayer(
            PLAYER_ID, 0, 10, MatchHistoryFilter.NONE
        ).content().getFirst();

        // The fixture stores red 13 and blue 7; a blue player must see 7 to 13.
        assertThat(response.allyScore()).isEqualTo(7);
        assertThat(response.enemyScore()).isEqualTo(13);
    }

    @Test
    @DisplayName("carries the page metadata through to the caller")
    void shouldCarryPageMetadataThrough() {
        given(List.of(match(10, 10, 0, 0, 0, 0, "Red")));

        PageResponse<MatchResponse> page =
            service.findByPlayer(PLAYER_ID, 0, 10, MatchHistoryFilter.NONE);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("rejects an untracked player before querying any match")
    void shouldRejectAnUntrackedPlayer() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(false);

        assertThatThrownBy(() ->
            service.findByPlayer(PLAYER_ID, 0, 10, MatchHistoryFilter.NONE))
            .isInstanceOf(PlayerNotFoundException.class);

        verifyNoInteractions(playerMatchRepository);
    }

    @Test
    @DisplayName("rejects an unknown filter value as a caller error, naming the accepted values")
    void shouldRejectUnknownFilterValues() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.findByPlayer(PLAYER_ID, 0, 10,
            new MatchHistoryFilter(null, null, null, "victory", null, null)))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("WIN");

        assertThatThrownBy(() -> service.findByPlayer(PLAYER_ID, 0, 10,
            new MatchHistoryFilter(null, null, null, null, "ranked", null)))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("gameMode");
    }

    @Test
    @DisplayName("rejects pagination outside the public contract before touching the database")
    void shouldRejectPaginationOutsideThePublicContract() {
        assertThatThrownBy(() ->
            service.findByPlayer(PLAYER_ID, -1, 10, MatchHistoryFilter.NONE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("page");

        assertThatThrownBy(() ->
            service.findByPlayer(PLAYER_ID, 0, 101, MatchHistoryFilter.NONE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("size");

        verifyNoInteractions(playerMatchRepository);
    }

    private PlayerMatch match(
        int kills,
        int deaths,
        int assists,
        int headshots,
        int bodyshots,
        int legshots,
        String teamId
    ) {
        ValorantMatch valorantMatch = new ValorantMatch();
        valorantMatch.setStartedAt(Instant.parse("2026-07-15T20:00:00Z"));
        valorantMatch.setMapName("Ascent");
        valorantMatch.setGameMode(GameMode.COMPETITIVE);
        valorantMatch.setRedScore(13);
        valorantMatch.setBlueScore(7);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setId(100L);
        playerMatch.setPlayer(player());
        playerMatch.setMatch(valorantMatch);
        playerMatch.setTeamId(teamId);
        playerMatch.setAgentName("Omen");
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(deaths);
        playerMatch.setAssists(assists);
        playerMatch.setHeadshots(headshots);
        playerMatch.setBodyshots(bodyshots);
        playerMatch.setLegshots(legshots);
        playerMatch.setAcs(BigDecimal.valueOf(230));
        playerMatch.setAdr(BigDecimal.valueOf(150));
        return playerMatch;
    }

    private Player player() {
        Player player = new Player();
        player.setId(PLAYER_ID);
        player.setDisplayName("natank");
        return player;
    }
}
