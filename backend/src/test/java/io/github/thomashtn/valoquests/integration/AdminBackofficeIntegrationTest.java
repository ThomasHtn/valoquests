package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.entity.Guardian;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.campaign.repository.GuardianRepository;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
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
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.profile.dto.PlayerSummaryResponse;
import io.github.thomashtn.valoquests.profile.service.PlayerQueryService;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.roster.dto.PlayerDeletionResponse;
import io.github.thomashtn.valoquests.roster.model.PlayerDeletionOutcome;
import io.github.thomashtn.valoquests.roster.service.PlayerAdminService;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.synchronization.entity.PlayerSeasonSynchronization;
import io.github.thomashtn.valoquests.synchronization.repository.PlayerSeasonSynchronizationRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the administration operations that touch the whole database against a real PostgreSQL.
 *
 * <p>Only Postgres validates the reset's {@code TRUNCATE} list, and four queries must agree on the archive status.
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
    private PlayerAdminService playerAdminService;

    @Autowired
    private PlayerSeasonSynchronizationRepository seasonSynchronizationRepository;

    @Autowired
    private EntityManager entityManager;

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
    private ChallengeSelectionRepository challengeSelectionRepository;

    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private CampaignWeekRepository campaignWeekRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Verifies that the reset empties every derived table while keeping the roster and catalogues.
     *
     * <p>The {@code TRUNCATE} lists every referencing table without {@code CASCADE}, so a new table fails here.
     */
    @Test
    void shouldClearEveryDerivedTableOnCampaignReset() {
        Player player = playerRepository.findAllByOrderByIdAsc().getFirst();
        player.setLastSuccessfulSynchronizationAt(MATCH_TIME);
        playerRepository.save(player);

        seedCampaignData(player);

        long challengeCatalogueSize = challengeRepository.count();
        long guardianCatalogueSize = guardianRepository.count();
        long rosterSize = playerRepository.count();

        campaignResetService.resetCampaign();

        assertThat(playerMatchRepository.count()).isZero();
        assertThat(valorantMatchRepository.count()).isZero();
        assertThat(seasonRepository.count()).isZero();
        assertThat(challengeSelectionRepository.count()).isZero();
        assertThat(progressRepository.count()).isZero();
        assertThat(scoreRepository.count()).isZero();
        assertThat(campaignWeekRepository.count()).isZero();
        assertThat(campaignPlayerRepository.count()).isZero();
        assertThat(campaignRepository.count()).isZero();

        assertThat(playerRepository.count()).isEqualTo(rosterSize);
        assertThat(challengeRepository.count()).isEqualTo(challengeCatalogueSize);
        assertThat(guardianRepository.count()).isEqualTo(guardianCatalogueSize);

        assertThat(playerRepository.findAllByOrderByIdAsc())
            .allSatisfy(kept ->
                assertThat(kept.getLastSuccessfulSynchronizationAt()).isNull()
            );
    }

    /**
     * Verifies that archiving a player takes it out of the roster without deleting it.
     *
     * <p>The public listing and synchronization scope drop it while the admin listing keeps it, via separate queries.
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

    @Test
    @DisplayName("Deletes a synchronized player no campaign counted, with its matches, scores and progress")
    void shouldDeleteASynchronizedPlayerOffEveryRoster() {
        Player player = new Player();
        player.setGameName("Benched");
        player.setTagLine("EUW");
        player.setDisplayName("Benched");
        player.setStatus(PlayerStatus.INACTIVE);
        player = playerRepository.save(player);
        long playerId = player.getId();

        PlayerMatch playerMatch = seedPlayerHistory(player);

        PlayerSeasonSynchronization checkpoint = new PlayerSeasonSynchronization();
        checkpoint.setPlayer(player);
        checkpoint.setSeason(playerMatch.getMatch().getSeason());
        seasonSynchronizationRepository.save(checkpoint);
        entityManager.flush();

        PlayerDeletionResponse response = playerAdminService.removeFromRoster(playerId);
        entityManager.flush();

        assertThat(response.outcome()).isEqualTo(PlayerDeletionOutcome.DELETED);
        assertThat(playerRepository.findById(playerId)).isEmpty();
        assertThat(playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId)).isEmpty();
        assertThat(progressRepository.findAllByPlayerId(playerId)).isEmpty();
        assertThat(scoreRepository.findAllByPlayerId(playerId)).isEmpty();
        assertThat(seasonSynchronizationRepository.findAllByPlayerId(playerId)).isEmpty();
    }

    @Test
    @DisplayName("Lists every player with its campaign contribution and recent play")
    void shouldFlagContributionAndRecentPlayInTheAdministrationListing() {
        Player veteran = playerRepository.findAllByOrderByIdAsc().getFirst();
        seedCampaignData(veteran);

        assertThat(playerAdminService.findAll())
            .allSatisfy(listed -> {
                boolean isVeteran = listed.id().equals(veteran.getId());
                assertThat(listed.wasOnAnyRoster()).isEqualTo(isVeteran);
                assertThat(listed.hasRecentMatch()).isFalse();
            });
        assertThat(playerMatchRepository.findPlayerIdsWithMatchStartedSince(MATCH_TIME))
            .containsExactly(veteran.getId());
        assertThat(playerMatchRepository.findPlayerIdsWithMatchStartedSince(MATCH_TIME.plusSeconds(1)))
            .isEmpty();
    }

    /**
     * Persists one row in each table the reset is expected to empty.
     *
     * @param player player the campaign data belongs to
     */
    private void seedCampaignData(Player player) {
        PlayerMatch playerMatch = seedPlayerHistory(player);

        Guardian guardian = guardianRepository
            .findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory.MINOR)
            .getFirst();

        Campaign campaign = new Campaign();
        campaign.setNumber(1);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setOpenedAt(MATCH_TIME);
        campaign.setFirstWeekStart(WEEK_START);
        campaign.setLastWeekStart(WEEK_START.plusWeeks(9));
        campaign.setRosterSize(7);
        campaign.setDifficulty(CampaignDifficulty.AMATEUR);
        campaignRepository.save(campaign);

        CampaignPlayer member = new CampaignPlayer();
        member.setCampaign(campaign);
        member.setPlayer(player);
        campaignPlayerRepository.save(member);

        CampaignWeek week = new CampaignWeek();
        week.setCampaign(campaign);
        week.setWeekIndex(1);
        week.setWeekStart(WEEK_START);
        week.setPlanetName("Orune");
        week.setCategory(GuardianCategory.MINOR);
        week.setGuardianWeight(new BigDecimal("0.60"));
        week.setGroupWeight(BigDecimal.ONE);
        week.setGuardian(guardian);
        week.setGuardianHitPoints(10_000);
        week.setWoundedCount(1_855);
        week.setDefeated(true);
        week.setDefeatedByPlayer(player);
        week.setFinishingPlayerMatch(playerMatch);
        campaignWeekRepository.save(week);
    }

    /**
     * Persists a match, a challenge progress and a weekly score for one player.
     *
     * @param player player the history belongs to
     * @return the stored player match
     */
    private PlayerMatch seedPlayerHistory(Player player) {
        Season season = new Season();
        season.setExternalId("admin-reset-season");
        season.setName("Admin Reset Season");
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

        ChallengeSelection selection = new ChallengeSelection();
        selection.setWeekStart(WEEK_START);
        selection.setChallenge(challenge);
        selection.setResolvedConditionsJson(challenge.getAmateurConditionsJson());
        selection.setSelectedAt(MATCH_TIME);
        selection = challengeSelectionRepository.save(selection);

        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setSelection(selection);
        progress.setCurrentValue(BigDecimal.ONE);
        progress.setTargetValue(BigDecimal.TEN);
        progress.setCompleted(false);
        progress.setCalculatedAt(MATCH_TIME);
        progressRepository.save(progress);

        WeeklyPlayerScore score = new WeeklyPlayerScore();
        score.setPlayer(player);
        score.setWeekStart(WEEK_START);
        score.setChallengePoints(100);
        score.setCompletedChallenges(0);
        score.setGuardianDamage(50);
        score.setTotalPoints(150);
        score.setPosition(1);
        score.setCalculatedAt(MATCH_TIME);
        scoreRepository.save(score);

        return playerMatch;
    }
}
