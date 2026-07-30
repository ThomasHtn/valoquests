package io.github.thomashtn.valorant.tracker.player.service;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
     * Service under test.
     */
    private DefaultPlayerQueryService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new DefaultPlayerQueryService(playerRepository, playerMatchRepository);
    }

    /**
     * Verifies that statistics are computed only from the matches the repository returns for the
     * requested season and game mode, forwarding both as parsed filters.
     */
    @Test
    void shouldScopeStatisticsToTheRequestedSeasonAndGameMode() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(1L, 5L, GameMode.COMPETITIVE))
            .thenReturn(List.of(match(MatchResult.WIN, "Jett", "Ascent")));

        PlayerDetailsResponse response = service.findById(1L, 5L, "competitive");

        assertThat(response.statistics().matchesPlayed()).isEqualTo(1);
        assertThat(response.statistics().wins()).isEqualTo(1);
        assertThat(response.statistics().losses()).isEqualTo(0);
    }

    /**
     * Verifies that omitting both filters reproduces the unfiltered, lifetime aggregate - a
     * regression guard for callers that still request every season and every mode.
     */
    @Test
    void shouldComputeLifetimeAggregateWhenNoFilterIsSupplied() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(1L, null, null))
            .thenReturn(List.of(
                match(MatchResult.WIN, "Jett", "Ascent"),
                match(MatchResult.LOSS, "Reyna", "Bind")
            ));

        PlayerDetailsResponse response = service.findById(1L, null, null);

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

        assertThatThrownBy(() -> service.findById(1L, null, "not-a-mode"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Verifies that requesting an untracked player still fails fast regardless of filters.
     */
    @Test
    void shouldThrowWhenPlayerDoesNotExist() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L, null, null))
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
