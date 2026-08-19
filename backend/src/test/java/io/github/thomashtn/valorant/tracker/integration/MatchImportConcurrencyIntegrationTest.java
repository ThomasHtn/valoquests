package io.github.thomashtn.valorant.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that {@link MatchImportService} stays idempotent under real concurrent execution against
 * PostgreSQL, where two synchronizations can genuinely race on the same database row.
 *
 * <p>Deliberately not wrapped in a per-test transaction: the whole point is that two separate threads
 * commit through two separate connections, exactly as two overlapping synchronizations would in
 * production. Each test cleans up the rows it created instead of relying on rollback.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
class MatchImportConcurrencyIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Henrik identifier of the season used by every match in this test.
     */
    private static final String SEASON_ID = "concurrency-season";

    @Autowired
    private MatchImportService matchImportService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    /**
     * JDBC client used to remove the season row this test class creates.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Removes every row this test class may have created, since nothing here rolls back.
     *
     * <p>Matched by identifier rather than by navigating {@code playerMatch.getMatch()}: that
     * association is lazy, and dereferencing it outside a Hibernate session would fail.
     */
    @AfterEach
    void tearDown() {
        List<Long> matchIds = valorantMatchRepository.findAll().stream()
            .filter(match -> match.getExternalMatchId().startsWith("concurrency-match-"))
            .map(ValorantMatch::getId)
            .toList();

        playerMatchRepository.deleteAll(
            playerMatchRepository.findAll().stream()
                .filter(playerMatch -> matchIds.contains(playerMatch.getMatch().getId()))
                .toList()
        );
        valorantMatchRepository.deleteAllById(matchIds);
        playerRepository.deleteAll(
            playerRepository.findAll().stream()
                .filter(player -> player.getRiotPuuid() != null
                    && player.getRiotPuuid().startsWith("concurrency-puuid-"))
                .toList()
        );
        jdbcTemplate.update("DELETE FROM season WHERE external_id = ?", SEASON_ID);
    }

    /**
     * Verifies that two different tracked players who both took part in the same match, imported
     * concurrently, end up sharing exactly one {@link ValorantMatch} row instead of one failing.
     */
    @Test
    void shouldShareOneMatchRowWhenTwoPlayersImportItConcurrently() throws Exception {
        Player playerA = createPlayer("concurrency-puuid-a");
        Player playerB = createPlayer("concurrency-puuid-b");

        HenrikMatchData sharedMatch = match("concurrency-match-shared", playerA, playerB);
        HenrikMatchHistoryResponse response =
            new HenrikMatchHistoryResponse(200, List.of(sharedMatch));

        runConcurrently(
            () -> matchImportService.importMatchesWithSummary(playerA, response),
            () -> matchImportService.importMatchesWithSummary(playerB, response)
        );

        List<ValorantMatch> storedMatches = valorantMatchRepository.findAll().stream()
            .filter(match -> "concurrency-match-shared".equals(match.getExternalMatchId()))
            .toList();
        assertThat(storedMatches).hasSize(1);

        List<PlayerMatch> storedAssociations = playerMatchRepository.findAll().stream()
            .filter(playerMatch -> playerMatch.getMatch().getId().equals(storedMatches.get(0).getId()))
            .toList();
        assertThat(storedAssociations).hasSize(2);
    }

    /**
     * Verifies that the same player imported concurrently, for instance a manual catch-up racing a
     * scheduled run, never creates two associations for the same match.
     */
    @Test
    void shouldCreateOnlyOneAssociationWhenTheSamePlayerImportsConcurrently() throws Exception {
        Player player = createPlayer("concurrency-puuid-single");

        HenrikMatchData onlyMatch = match("concurrency-match-single", player, null);
        HenrikMatchHistoryResponse response =
            new HenrikMatchHistoryResponse(200, List.of(onlyMatch));

        runConcurrently(
            () -> matchImportService.importMatchesWithSummary(player, response),
            () -> matchImportService.importMatchesWithSummary(player, response)
        );

        List<PlayerMatch> storedAssociations = playerMatchRepository.findAll().stream()
            .filter(playerMatch -> playerMatch.getPlayer().getId().equals(player.getId()))
            .toList();
        assertThat(storedAssociations).hasSize(1);
    }

    /**
     * Runs two import calls on separate threads, synchronized to start at the same instant so the
     * database race is actually exercised instead of the two calls happening to run sequentially.
     */
    private void runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = List.of(
                executor.submit(() -> awaitAndRun(barrier, first)),
                executor.submit(() -> awaitAndRun(barrier, second))
            );
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Object awaitAndRun(CyclicBarrier barrier, Callable<?> task) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
            return task.call();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Persists a minimal tracked player.
     */
    private Player createPlayer(String puuid) {
        Player player = new Player();
        player.setRiotPuuid(puuid);
        player.setGameName(puuid);
        player.setTagLine("EUW");
        player.setDisplayName(puuid);
        player.setStatus(PlayerStatus.ACTIVE);
        return playerRepository.save(player);
    }

    /**
     * Creates a completed Henrik match, optionally shared by two tracked players.
     */
    private HenrikMatchData match(String matchId, Player playerA, Player playerB) {
        List<HenrikMatchPlayer> players = playerB == null
            ? List.of(henrikPlayer(playerA, "Red"))
            : List.of(henrikPlayer(playerA, "Red"), henrikPlayer(playerB, "Blue"));

        return new HenrikMatchData(
            new HenrikMatchMetadata(
                matchId,
                new HenrikMatchMetadata.HenrikMap("map-1", "Ascent"),
                1_800_000L,
                Instant.parse("2026-07-20T18:00:00Z"),
                true,
                new HenrikMatchMetadata.HenrikQueue("competitive", null, null),
                new HenrikMatchMetadata.HenrikSeason(SEASON_ID, "V26 Act 4")
            ),
            players,
            List.of(
                new HenrikMatchTeam("Red", true, new HenrikMatchTeam.HenrikRounds(13, 7)),
                new HenrikMatchTeam("Blue", false, new HenrikMatchTeam.HenrikRounds(7, 13))
            )
        );
    }

    private HenrikMatchPlayer henrikPlayer(Player player, String teamId) {
        return new HenrikMatchPlayer(
            player.getRiotPuuid(),
            player.getGameName(),
            player.getTagLine(),
            teamId,
            new HenrikMatchPlayer.HenrikAgent("agent-1", "Jett"),
            new HenrikMatchPlayer.HenrikPlayerStats(
                4000, 20, 12, 3, 10, 25, 2,
                new HenrikMatchPlayer.HenrikDamage(3200, 2800)
            ),
            new HenrikMatchPlayer.HenrikTier(21, "Immortal 1")
        );
    }
}
