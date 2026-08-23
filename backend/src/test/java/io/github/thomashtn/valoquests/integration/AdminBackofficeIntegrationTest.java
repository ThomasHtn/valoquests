package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.maintenance.service.CampaignResetService;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.repository.SeasonRepository;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valoquests.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.player.service.PlayerQueryService;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the administration operations that touch the whole database against a real PostgreSQL.
 *
 * <p>These two behaviours cannot be trusted from unit tests. The campaign reset is a Postgres
 * multi-table {@code TRUNCATE} whose table list is only validated by Postgres itself, and archiving
 * a player is a single status value that four different queries have to agree on.
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
class AdminBackofficeIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Monday identifying the week built by the test.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 13);

    /**
     * Instant a match of that week was played at.
     */
    private static final Instant MATCH_TIME = Instant.parse("2026-07-15T20:00:00Z");

    @Autowired
    private CampaignResetService campaignResetService;

    @Autowired
    private PlayerQueryService playerQueryService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    @Autowired
    private BossCatalogEntryRepository bossCatalogEntryRepository;

    @Autowired
    private WeeklyBossEncounterRepository bossEncounterRepository;

    /**
     * Verifies that the reset empties every derived table while keeping the roster and catalogues.
     *
     * <p>The {@code TRUNCATE} deliberately omits {@code CASCADE} and lists every referencing table
     * instead, so Postgres rejects it outright if the list is incomplete. That is what this test
     * actually exercises: a future table referencing one of these would make it fail here rather
     * than silently leave orphaned campaign data behind in production.
     */
    @Test
    void shouldClearEveryDerivedTableOnCampaignReset() {
        Player player = playerRepository.findAllByOrderByIdAsc().getFirst();
        player.setLastSuccessfulSynchronizationAt(MATCH_TIME);
        playerRepository.save(player);

        seedCampaignData(player);

        long challengeCatalogueSize = challengeRepository.count();
        long bossCatalogueSize = bossCatalogEntryRepository.count();
        long rosterSize = playerRepository.count();

        campaignResetService.resetCampaign();

        assertThat(playerMatchRepository.count()).isZero();
        assertThat(valorantMatchRepository.count()).isZero();
        assertThat(seasonRepository.count()).isZero();
        assertThat(weeklyChallengeRepository.count()).isZero();
        assertThat(progressRepository.count()).isZero();
        assertThat(scoreRepository.count()).isZero();
        assertThat(bossEncounterRepository.count()).isZero();

        assertThat(playerRepository.count()).isEqualTo(rosterSize);
        assertThat(challengeRepository.count()).isEqualTo(challengeCatalogueSize);
        assertThat(bossCatalogEntryRepository.count()).isEqualTo(bossCatalogueSize);

        assertThat(playerRepository.findAllByOrderByIdAsc())
            .allSatisfy(kept ->
                assertThat(kept.getLastSuccessfulSynchronizationAt()).isNull()
            );
    }

    /**
     * Verifies that archiving a player takes it out of the roster without deleting it.
     *
     * <p>The public listing and the synchronization scope must both drop it, while the
     * administration listing keeps it — that is the whole point of the status, and the three
     * queries reading it are separate.
     */
    @Test
    void shouldKeepAnArchivedPlayerOutOfTheRosterButStillStored() {
        Player player = playerRepository.findAllByOrderByIdAsc().getFirst();
        player.setStatus(PlayerStatus.ARCHIVED);
        playerRepository.save(player);

        assertThat(playerQueryService.findAll())
            .extracting(PlayerSummaryResponse::id)
            .doesNotContain(player.getId());

        assertThat(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .extracting(Player::getId)
            .doesNotContain(player.getId());

        assertThat(playerRepository.findAllByOrderByIdAsc())
            .extracting(Player::getId)
            .contains(player.getId());

        assertThat(playerRepository.findById(player.getId())).isPresent();
    }

    /**
     * Persists one row in each table the reset is expected to empty.
     *
     * @param player player the campaign data belongs to
     */
    private void seedCampaignData(Player player) {
        Season season = new Season();
        season.setExternalId("admin-reset-season");
        season.setName("Admin Reset Season");
        season.setStartsAt(MATCH_TIME.minusSeconds(86_400));
        season.setEndsAt(MATCH_TIME.plusSeconds(86_400));
        season.setActive(true);
        season = seasonRepository.save(season);

        ValorantMatch match = new ValorantMatch();
        match.setExternalMatchId("admin-reset-match");
        match.setSeason(season);
        match.setStartedAt(MATCH_TIME);
        match.setDurationSeconds(2_400);
        match.setMapId("ascent");
        match.setMapName("Ascent");
        match.setGameMode(GameMode.COMPETITIVE);
        match.setGameModeSource(GameModeSource.PROVIDED);
        match.setQueueId("competitive");
        match.setRedScore(13);
        match.setBlueScore(10);
        match = valorantMatchRepository.save(match);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setPlayer(player);
        playerMatch.setMatch(match);
        playerMatch.setTeamId("Blue");
        playerMatch.setAgentName("Omen");
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(20);
        playerMatch.setDeaths(10);
        playerMatch.setAssists(5);
        playerMatch.setScore(5_000);
        playerMatch.setHeadshots(8);
        playerMatch.setBodyshots(20);
        playerMatch.setLegshots(0);
        playerMatch.setDamageDealt(4_000);
        playerMatch.setRoundsPlayed(23);
        playerMatch.setMvp(false);
        playerMatch = playerMatchRepository.save(playerMatch);

        Challenge challenge = challengeRepository.findAll().getFirst();

        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(MATCH_TIME);
        weeklyChallenge = weeklyChallengeRepository.save(weeklyChallenge);

        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCurrentValue(BigDecimal.ONE);
        progress.setTargetValue(BigDecimal.TEN);
        progress.setCompleted(false);
        progress.setCalculatedAt(MATCH_TIME);
        progressRepository.save(progress);

        WeeklyPlayerScore score = new WeeklyPlayerScore();
        score.setPlayer(player);
        score.setWeekStart(WEEK_START);
        score.setChallengeDamage(100);
        score.setCompletedChallenges(0);
        score.setMatchDamage(50);
        score.setRegularityBonus(0);
        score.setTeamBonus(0);
        score.setTotalDamage(150);
        score.setActiveDays(1);
        score.setPosition(1);
        score.setCalculatedAt(MATCH_TIME);
        scoreRepository.save(score);

        BossCatalogEntry bossEntry = bossCatalogEntryRepository.findAll().getFirst();

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(WEEK_START);
        encounter.setBossCatalogEntry(bossEntry);
        encounter.setRulesetVersion(1);
        encounter.setBaseHp(10_000);
        encounter.setDifficultyModifierPercent(0);
        encounter.setEffectiveHp(10_000);
        encounter.setDefeated(true);
        encounter.setDefeatedByPlayer(player);
        encounter.setFinishingPlayerMatch(playerMatch);
        bossEncounterRepository.save(encounter);
    }
}
