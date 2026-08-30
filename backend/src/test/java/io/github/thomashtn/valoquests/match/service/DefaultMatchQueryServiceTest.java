package io.github.thomashtn.valoquests.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.match.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.match.dto.MatchResponse;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver.MatchDamage;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Monday the fixture matches belong to.
     */
    private static final LocalDate FIXTURE_WEEK_START = LocalDate.of(2026, 7, 13);

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMatchRepository playerMatchRepository;

    @Mock
    private WeekCalendar weekCalendar;

    @Mock
    private WeeklyMatchDamageResolver damageResolver;

    @Mock
    private ScoringRuleset ruleset;

    private DefaultMatchQueryService service;

    @BeforeEach
    void setUp() {
        service = new DefaultMatchQueryService(
            playerRepository, playerMatchRepository, weekCalendar, damageResolver, ruleset
        );
    }

    /**
     * Makes the player exist and the repository return the supplied matches.
     *
     * <p>A non-empty page is also given a pricing pass that finds nothing, so the tests reading the
     * Valorant statistics do not each have to describe a scoring run they are not about.
     */
    private void given(List<PlayerMatch> matches) {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(playerMatchRepository.findHistory(
            any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(matches, PageRequest.of(0, 10), matches.size()));

        if (!matches.isEmpty()) {
            when(weekCalendar.weekStartOf(any(Instant.class))).thenReturn(FIXTURE_WEEK_START);
            when(playerMatchRepository.findForChallengePeriod(any(), any(), any()))
                .thenReturn(List.of());
            when(damageResolver.resolveDetailed(any(), any())).thenReturn(Map.of());
        }
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
    @DisplayName("prices a match against its whole week rather than against the page it landed on")
    void shouldPriceAMatchAgainstItsWholeWeek() {
        PlayerMatch pageMatch = match(20, 8, 4, 30, 60, 10, "Red");
        Instant periodStart = Instant.parse("2026-07-13T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-07-20T00:00:00Z");

        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        // One match per page: whatever rank this one holds within its day, the page it shipped on
        // cannot say, since the rest of the day is on other pages entirely.
        when(playerMatchRepository.findHistory(any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pageMatch), PageRequest.of(0, 1), 11));
        when(weekCalendar.weekStartOf(any(Instant.class))).thenReturn(FIXTURE_WEEK_START);
        when(weekCalendar.startOf(FIXTURE_WEEK_START)).thenReturn(periodStart);
        when(weekCalendar.endOf(FIXTURE_WEEK_START)).thenReturn(periodEnd);
        when(playerMatchRepository.findForChallengePeriod(PLAYER_ID, periodStart, periodEnd))
            .thenReturn(List.of(pageMatch));
        when(damageResolver.resolveDetailed(List.of(pageMatch), ruleset))
            .thenReturn(Map.of(100L, new MatchDamage(125, 25)));

        MatchResponse response = service.findByPlayer(
            PLAYER_ID, 0, 1, MatchHistoryFilter.NONE
        ).content().getFirst();

        assertThat(response.valoquestsDamage()).isEqualTo(125);
        assertThat(response.damageCoefficientPercent()).isEqualTo(25);
        verify(playerMatchRepository).findForChallengePeriod(PLAYER_ID, periodStart, periodEnd);
    }

    @Test
    @DisplayName("reports no damage for a match the ruleset never priced")
    void shouldReportNoDamageForAnUnpricedMatch() {
        given(List.of(match(10, 10, 0, 0, 0, 0, "Red")));

        MatchResponse response = service.findByPlayer(
            PLAYER_ID, 0, 10, MatchHistoryFilter.NONE
        ).content().getFirst();

        assertThat(response.valoquestsDamage()).isZero();
        assertThat(response.damageCoefficientPercent()).isZero();
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

    @Test
    @DisplayName("returns full detail for one of the player's own matches")
    void shouldReturnFullDetailForOneOfThePlayersOwnMatches() {
        PlayerMatch playerMatch = match(20, 8, 4, 30, 60, 10, "Red");
        playerMatch.setDamageDealt(2500);
        playerMatch.setRoundsPlayed(24);
        playerMatch.setMvp(true);
        playerMatch.getMatch().setDurationSeconds(2100);

        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(playerMatchRepository.findByIdAndPlayerId(100L, PLAYER_ID))
            .thenReturn(Optional.of(playerMatch));
        when(weekCalendar.weekStartOf(any(Instant.class))).thenReturn(FIXTURE_WEEK_START);
        when(playerMatchRepository.findForChallengePeriod(any(), any(), any()))
            .thenReturn(List.of(playerMatch));
        when(damageResolver.resolveDetailed(List.of(playerMatch), ruleset))
            .thenReturn(Map.of(100L, new MatchDamage(125, 25)));
        when(playerMatchRepository.findByMatchIdAndPlayerIdNot(any(), eq(PLAYER_ID)))
            .thenReturn(List.of());

        MatchDetailResponse response = service.findDetail(PLAYER_ID, 100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.durationSeconds()).isEqualTo(2100);
        assertThat(response.headshots()).isEqualTo(30);
        assertThat(response.bodyshots()).isEqualTo(60);
        assertThat(response.legshots()).isEqualTo(10);
        assertThat(response.damageDealt()).isEqualTo(2500);
        assertThat(response.roundsPlayed()).isEqualTo(24);
        assertThat(response.mvp()).isTrue();
        assertThat(response.kda()).isEqualByComparingTo("3.00");
        assertThat(response.headshotPercentage()).isEqualByComparingTo("30.00");
        assertThat(response.valoquestsDamage()).isEqualTo(125);
        assertThat(response.damageCoefficientPercent()).isEqualTo(25);
        assertThat(response.teammates()).isEmpty();
    }

    @Test
    @DisplayName("lists other tracked players found in the same match, flagging shared teams")
    void shouldListOtherTrackedPlayersFoundInTheSameMatch() {
        PlayerMatch playerMatch = match(20, 8, 4, 30, 60, 10, "Red");

        PlayerMatch ally = match(10, 5, 6, 10, 20, 5, "Red");
        ally.setId(101L);
        ally.setMatch(playerMatch.getMatch());
        Player allyPlayer = new Player();
        allyPlayer.setId(2L);
        allyPlayer.setDisplayName("teammate");
        allyPlayer.setPortrait("Jett");
        ally.setPlayer(allyPlayer);

        PlayerMatch opponent = match(8, 12, 2, 5, 15, 5, "Blue");
        opponent.setId(102L);
        opponent.setMatch(playerMatch.getMatch());
        Player opponentPlayer = new Player();
        opponentPlayer.setId(3L);
        opponentPlayer.setDisplayName("rival");
        opponent.setPlayer(opponentPlayer);

        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(playerMatchRepository.findByIdAndPlayerId(100L, PLAYER_ID))
            .thenReturn(Optional.of(playerMatch));
        when(weekCalendar.weekStartOf(any(Instant.class))).thenReturn(FIXTURE_WEEK_START);
        when(playerMatchRepository.findForChallengePeriod(any(), any(), any()))
            .thenReturn(List.of(playerMatch));
        when(damageResolver.resolveDetailed(any(), any())).thenReturn(Map.of());
        when(playerMatchRepository.findByMatchIdAndPlayerIdNot(any(), eq(PLAYER_ID)))
            .thenReturn(List.of(ally, opponent));

        MatchDetailResponse response = service.findDetail(PLAYER_ID, 100L);

        assertThat(response.teammates()).hasSize(2);
        assertThat(response.teammates())
            .filteredOn(teammate -> teammate.playerId().equals(2L))
            .singleElement()
            .satisfies(teammate -> {
                assertThat(teammate.displayName()).isEqualTo("teammate");
                assertThat(teammate.sameTeam()).isTrue();
            });
        assertThat(response.teammates())
            .filteredOn(teammate -> teammate.playerId().equals(3L))
            .singleElement()
            .satisfies(teammate -> assertThat(teammate.sameTeam()).isFalse());
    }

    @Test
    @DisplayName("rejects an untracked player before querying any match")
    void shouldRejectDetailForAnUntrackedPlayer() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.findDetail(PLAYER_ID, 100L))
            .isInstanceOf(PlayerNotFoundException.class);

        verifyNoInteractions(playerMatchRepository);
    }

    @Test
    @DisplayName("rejects a match that does not belong to the requesting player")
    void shouldRejectAMatchThatDoesNotBelongToThePlayer() {
        when(playerRepository.existsById(PLAYER_ID)).thenReturn(true);
        when(playerMatchRepository.findByIdAndPlayerId(999L, PLAYER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail(PLAYER_ID, 999L))
            .isInstanceOf(MatchNotFoundException.class);
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
