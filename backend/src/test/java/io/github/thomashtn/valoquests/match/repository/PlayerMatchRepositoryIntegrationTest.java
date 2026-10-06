package io.github.thomashtn.valoquests.match.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.integration.PostgreSqlIntegrationTest;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignPlayerRepository campaignPlayerRepository;

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
            new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            ),
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
            new PlayerMatchHistoryCriteria(
                null, "ascent", "jett", null, null,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            ),
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
            new PlayerMatchHistoryCriteria(
                null, null, null, null, GameMode.COMPETITIVE,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            ),
            PageRequest.of(0, 10)
        );
        Page<PlayerMatch> deathmatch = playerMatchRepository.findHistory(
            player.getId(),
            new PlayerMatchHistoryCriteria(
                null, null, null, null, GameMode.DEATHMATCH,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            ),
            PageRequest.of(0, 10)
        );

        assertThat(competitive.getTotalElements()).isEqualTo(1);
        assertThat(deathmatch.getTotalElements()).isZero();
    }

    /**
     * Ensures the {@code periodStart}/{@code periodEnd} range keeps matches inside the week and
     * excludes matches outside it, mirroring {@link PlayerMatchRepository#findByPlayerInPeriod}.
     */
    @Test
    void shouldFilterHistoryByWeekPeriod() {
        Player player = createPlayer();
        Season season = createSeason();
        ValorantMatch match = createMatch(season);
        playerMatchRepository.save(createPlayerMatch(player, match));

        Page<PlayerMatch> insideWeek = playerMatchRepository.findHistory(
            player.getId(),
            new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                Instant.parse("2026-07-20T00:00:00Z"),
                Instant.parse("2026-07-27T00:00:00Z")
            ),
            PageRequest.of(0, 10)
        );
        Page<PlayerMatch> outsideWeek = playerMatchRepository.findHistory(
            player.getId(),
            new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                Instant.parse("2026-07-27T00:00:00Z"),
                Instant.parse("2026-08-03T00:00:00Z")
            ),
            PageRequest.of(0, 10)
        );

        assertThat(insideWeek.getTotalElements()).isEqualTo(1);
        assertThat(outsideWeek.getTotalElements()).isZero();
    }

    /**
     * Exercises {@link PlayerMatchRepository#findHistory} unpaged, as the profile statistics read
     * it, against real PostgreSQL: a narrow period keeps only the matches inside it, and
     * {@link PlayerMatchHistoryCriteria#UNBOUNDED_PERIOD_START}/{@link
     * PlayerMatchHistoryCriteria#UNBOUNDED_PERIOD_END} (what callers use in place of a week filter)
     * still return every match regardless of date.
     *
     * <p>Regression guard for a bug where the period was bound through a
     * {@code :param IS NULL OR ...} check: PostgreSQL 16 rejected it with "could not determine data
     * type of parameter", since that placeholder's only usage in the query text carried no type
     * information at statement-prepare time - independent of whether the bound value later turned
     * out to be null. See {@link PlayerMatchHistoryCriteria}'s Javadoc for the full explanation and
     * why the fix is an unconditional comparison rather than a cast.
     */
    @Test
    void shouldFilterStatisticsByWeekPeriodWithoutAPostgresTypeInferenceError() {
        Player player = createPlayer();
        Season season = createSeason();
        ValorantMatch match = createMatch(season);
        playerMatchRepository.save(createPlayerMatch(player, match));
        Pageable everyMatch = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "match.startedAt", "id"));

        List<PlayerMatch> insideWeek = playerMatchRepository.findHistory(
            player.getId(),
            new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                Instant.parse("2026-07-20T00:00:00Z"),
                Instant.parse("2026-07-27T00:00:00Z")
            ),
            everyMatch
        ).getContent();
        List<PlayerMatch> outsideWeek = playerMatchRepository.findHistory(
            player.getId(),
            new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                Instant.parse("2026-07-27T00:00:00Z"),
                Instant.parse("2026-08-03T00:00:00Z")
            ),
            everyMatch
        ).getContent();
        List<PlayerMatch> unbounded = playerMatchRepository.findHistory(
            player.getId(),
            new PlayerMatchHistoryCriteria(
                null, null, null, null, null,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
                PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
            ),
            everyMatch
        ).getContent();

        assertThat(unbounded).hasSize(1);
        assertThat(insideWeek).hasSize(1);
        assertThat(outsideWeek).isEmpty();
    }

    /**
     * Ensures the squad history keeps the campaign roster's matches of the period only, archived
     * members left out, sorted on the Valorant match's start as the service asks for.
     */
    @Test
    void shouldListSquadHistoryForTheCampaignRosterOverThePeriod() {
        Player member = createPlayer();
        Player archivedMember = createPlayer("archived", PlayerStatus.ARCHIVED);
        Player outsider = createPlayer("outsider", PlayerStatus.ACTIVE);
        ValorantMatch match = createMatch(createSeason());
        playerMatchRepository.save(createPlayerMatch(member, match));
        playerMatchRepository.save(createPlayerMatch(archivedMember, match));
        playerMatchRepository.save(createPlayerMatch(outsider, match));
        Campaign campaign = new Campaign();
        campaign.setNumber(1);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setOpenedAt(Instant.parse("2026-07-10T10:00:00Z"));
        campaign.setFirstWeekStart(LocalDate.of(2026, 7, 13));
        campaign.setLastWeekStart(LocalDate.of(2026, 9, 14));
        campaign.setRosterSize(2);
        campaign.setDifficulty(CampaignDifficulty.AMATEUR);
        campaign = campaignRepository.save(campaign);
        for (Player player : List.of(member, archivedMember)) {
            CampaignPlayer rosterEntry = new CampaignPlayer();
            rosterEntry.setCampaign(campaign);
            rosterEntry.setPlayer(player);
            campaignPlayerRepository.save(rosterEntry);
        }
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "match.startedAt", "id");

        Page<PlayerMatch> sameDay = playerMatchRepository.findSquadHistory(
            campaign.getId(),
            EnumSet.of(PlayerStatus.ACTIVE, PlayerStatus.INACTIVE),
            Instant.parse("2026-07-20T00:00:00Z"),
            Instant.parse("2026-07-21T00:00:00Z"),
            PageRequest.of(0, 10, newestFirst)
        );
        Page<PlayerMatch> nextDay = playerMatchRepository.findSquadHistory(
            campaign.getId(),
            EnumSet.of(PlayerStatus.ACTIVE, PlayerStatus.INACTIVE),
            Instant.parse("2026-07-21T00:00:00Z"),
            Instant.parse("2026-07-22T00:00:00Z"),
            PageRequest.of(0, 10, newestFirst)
        );

        assertThat(sameDay.getContent())
            .extracting(playerMatch -> playerMatch.getPlayer().getId())
            .containsExactly(member.getId());
        assertThat(nextDay).isEmpty();
    }

    private Player createPlayer(String name, PlayerStatus status) {
        Player player = new Player();

        player.setRiotPuuid("player-match-repository-test-" + name + "-puuid");
        player.setGameName(name);
        player.setTagLine("TEST");
        player.setDisplayName(name + "#TEST");
        player.setStatus(status);

        return playerRepository.save(player);
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
