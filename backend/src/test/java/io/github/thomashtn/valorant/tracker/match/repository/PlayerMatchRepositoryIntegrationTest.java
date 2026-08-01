package io.github.thomashtn.valorant.tracker.match.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valorant.tracker.integration.PostgreSqlIntegrationTest;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.GameModeSource;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies {@link PlayerMatchRepository#findHistory} against PostgreSQL.
 *
 * <p>Regression coverage for a bug where optional {@code map}/{@code agent} filters bound as
 * {@code null} made Hibernate send an untyped parameter into {@code LOWER(...)}, which PostgreSQL
 * resolved as {@code bytea} and rejected with {@code function lower(bytea) does not exist}.</p>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Transactional
class PlayerMatchRepositoryIntegrationTest
    extends PostgreSqlIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Ensures the {@code map}/{@code agent} filters accept {@code null} without a type-resolution
     * error from PostgreSQL.
     */
    @Test
    void shouldReturnHistoryWhenOptionalFiltersAreNull() {
        Player player = createPlayer();
        Season season = createSeason();
        ValorantMatch match = createMatch(season);
        playerMatchRepository.save(createPlayerMatch(player, match));

        Page<PlayerMatch> history = playerMatchRepository.findHistory(
            player.getId(),
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertThat(history.getTotalElements()).isEqualTo(1);
    }

    /**
     * Ensures the {@code map}/{@code agent} filters still apply a case-insensitive match when set.
     */
    @Test
    void shouldFilterHistoryByMapAndAgentIgnoringCase() {
        Player player = createPlayer();
        Season season = createSeason();
        ValorantMatch match = createMatch(season);
        playerMatchRepository.save(createPlayerMatch(player, match));

        Page<PlayerMatch> history = playerMatchRepository.findHistory(
            player.getId(),
            null,
            "ascent",
            "jett",
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertThat(history.getTotalElements()).isEqualTo(1);
    }

    /**
     * Ensures the {@code gameMode} filter keeps matching modes and excludes the others.
     */
    @Test
    void shouldFilterHistoryByGameMode() {
        Player player = createPlayer();
        Season season = createSeason();
        ValorantMatch match = createMatch(season);
        playerMatchRepository.save(createPlayerMatch(player, match));

        Page<PlayerMatch> competitive = playerMatchRepository.findHistory(
            player.getId(),
            null,
            null,
            null,
            null,
            GameMode.COMPETITIVE,
            PageRequest.of(0, 10)
        );
        Page<PlayerMatch> deathmatch = playerMatchRepository.findHistory(
            player.getId(),
            null,
            null,
            null,
            null,
            GameMode.DEATHMATCH,
            PageRequest.of(0, 10)
        );

        assertThat(competitive.getTotalElements()).isEqualTo(1);
        assertThat(deathmatch.getTotalElements()).isZero();
    }

    private Player createPlayer() {
        Player player = new Player();

        player.setRiotPuuid("player-match-repository-test-puuid");
        player.setGameName("RepositoryTestPlayer");
        player.setTagLine("TEST");
        player.setDisplayName("RepositoryTestPlayer#TEST");
        player.setStatus(PlayerStatus.ACTIVE);

        return playerRepository.save(player);
    }

    private Season createSeason() {
        Season season = new Season();

        season.setExternalId("player-match-repository-test-season");
        season.setName("Repository Test Season");
        season.setStartsAt(Instant.parse("2026-07-01T00:00:00Z"));
        season.setEndsAt(Instant.parse("2026-08-31T23:59:59Z"));
        season.setActive(true);

        return seasonRepository.save(season);
    }

    private ValorantMatch createMatch(Season season) {
        ValorantMatch match = new ValorantMatch();

        match.setExternalMatchId("player-match-repository-test-match");
        match.setSeason(season);
        match.setStartedAt(Instant.parse("2026-07-20T18:00:00Z"));
        match.setDurationSeconds(2_400);
        match.setMapId("ascent");
        match.setMapName("Ascent");
        match.setGameMode(GameMode.COMPETITIVE);
        match.setGameModeSource(GameModeSource.PROVIDED);
        match.setQueueId("competitive");
        match.setRedScore(13);
        match.setBlueScore(10);

        return valorantMatchRepository.save(match);
    }

    private PlayerMatch createPlayerMatch(Player player, ValorantMatch match) {
        PlayerMatch playerMatch = new PlayerMatch();

        playerMatch.setPlayer(player);
        playerMatch.setMatch(match);
        playerMatch.setTeamId("Blue");
        playerMatch.setAgentId("jett");
        playerMatch.setAgentName("Jett");
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(20);
        playerMatch.setDeaths(10);
        playerMatch.setAssists(5);
        playerMatch.setScore(5_000);
        playerMatch.setHeadshots(10);
        playerMatch.setBodyshots(20);
        playerMatch.setLegshots(0);
        playerMatch.setDamageDealt(3_000);
        playerMatch.setRoundsPlayed(23);
        playerMatch.setMvp(true);

        return playerMatch;
    }
}
