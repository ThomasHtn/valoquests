package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignSchedule;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeCalibrationSource;
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
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drives one campaign through its whole life on a real PostgreSQL: opened from the backoffice,
 * started and settled by the production rollover, closed after its tenth Sunday, then a second one
 * stopped early and deleted.
 *
 * <p>What unit tests cannot vouch for: that the calibration, the roster freeze, the guardian draw,
 * the replay and the closing agree on the same rows once every service runs against the migrated
 * schema, and that the campaign's own reference is the one the challenges are priced at.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=" + CampaignLifecycleIntegrationTest.ADMIN_KEY,
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false",
        "app.scheduling.campaign-tick-enabled=false"
    }
)
@AutoConfigureMockMvc
@Import(CampaignLifecycleIntegrationTest.MutableClockConfiguration.class)
@Transactional
class CampaignLifecycleIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Admin key the backoffice routes are called with.
     */
    static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    /**
     * Wednesday the campaign is opened on.
     */
    private static final Instant OPENING_TIME = Instant.parse("2026-07-15T12:00:00Z");

    /**
     * Monday the campaign starts on: the one after the opening day.
     */
    private static final LocalDate FIRST_WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Sunday closing the tenth week.
     */
    private static final LocalDate FINAL_DAY = FIRST_WEEK_START.plusWeeks(CampaignSchedule.WEEK_COUNT).minusDays(1);

    /**
     * Rollover instant of the first Monday.
     */
    private static final Instant START_TIME = Instant.parse("2026-07-20T00:05:00Z");

    /**
     * Rollover instant of the Monday after the final day.
     */
    private static final Instant CLOSING_TIME = Instant.parse("2026-09-28T00:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock mutableClock;

    @Autowired
    private CampaignLifecycleService lifecycleService;

    @Autowired
    private WeeklyRolloverService weeklyRolloverService;

    @Autowired
    private ChallengeCalibrationSource calibrationSource;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignPlayerRepository campaignPlayerRepository;

    @Autowired
    private CampaignWeekRepository campaignWeekRepository;

    @Autowired
    private CampaignDailySnapshotRepository snapshotRepository;

    @Autowired
    private CampaignPlayerDayRepository playerDayRepository;

    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    @Autowired
    private EntityManager entityManager;

    private Player alpha;

    private Player bravo;

    private Player charlie;

    private Season season;

    @BeforeEach
    void setUp() {
        mutableClock.setInstant(OPENING_TIME);
        // The migrations seed the real squad: only the three players below take part here.
        playerRepository.findAll().forEach(seeded -> seeded.setStatus(PlayerStatus.ARCHIVED));
        alpha = createPlayer("lifecycle-alpha", "Alpha", PlayerStatus.ACTIVE);
        bravo = createPlayer("lifecycle-bravo", "Bravo", PlayerStatus.ACTIVE);
        charlie = createPlayer("lifecycle-charlie", "Charlie", PlayerStatus.INACTIVE);
        season = createSeason();
        // Alpha alone has history before the opening: the calibration covers them, Bravo is a beginner.
        playCompetitiveMatches(alpha, LocalDate.of(2026, 6, 2), 2);
    }

    @Test
    @DisplayName("Opens a campaign on the active roster from the backoffice, once")
    void shouldOpenACampaignOnTheActiveRoster() throws Exception {
        mockMvc.perform(get("/api/admin/campaigns/calibration").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reference").value(2_000))
            .andExpect(jsonPath("$.tier").value("AMATEUR"))
            .andExpect(jsonPath("$.players.length()").value(2))
            .andExpect(jsonPath("$.players[?(@.displayName == 'Alpha#TEST')].beginner").value(false))
            .andExpect(jsonPath("$.players[?(@.displayName == 'Bravo#TEST')].beginner").value(true));

        mockMvc.perform(post("/api/admin/campaigns").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.status").value("OPENED"))
            .andExpect(jsonPath("$.firstWeekStart").value(FIRST_WEEK_START.toString()))
            .andExpect(jsonPath("$.lastWeekStart").value(FIRST_WEEK_START.plusWeeks(9).toString()))
            .andExpect(jsonPath("$.rosterSize").value(2));

        mockMvc.perform(post("/api/admin/campaigns").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isConflict());

        Campaign campaign = campaignRepository.findByStatusNot(CampaignStatus.CLOSED).orElseThrow();
        assertThat(campaign.getReference()).isEqualTo(2_000);
        assertThat(campaign.getTier()).isEqualTo(CampaignTier.AMATEUR);
        assertThat(campaign.getOpenedAt()).isEqualTo(OPENING_TIME);

        List<CampaignPlayer> roster = campaignPlayerRepository.findAllByCampaignIdOrderByPlayerIdAsc(campaign.getId());
        assertThat(roster).extracting(member -> member.getPlayer().getId())
            .containsExactly(alpha.getId(), bravo.getId());
        assertThat(campaignPlayerRepository.existsByPlayerId(charlie.getId())).isFalse();

        List<CampaignWeek> weeks = campaignWeekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());
        assertThat(weeks).hasSize(CampaignSchedule.WEEK_COUNT);
        assertThat(weeks).extracting(CampaignWeek::getWeekStart)
            .containsExactlyElementsOf(weekStarts());
        assertThat(weeks).extracting(week -> week.getGuardian().getId()).doesNotHaveDuplicates();
        assertThat(weeks).allSatisfy(week -> {
            assertThat(week.getGuardianHitPoints()).isPositive();
            assertThat(week.getWoundedCount()).isPositive();
            assertThat(week.isSettled()).isFalse();
        });

        mockMvc.perform(get("/api/campaign"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPENED"))
            .andExpect(jsonPath("$.currentWeekIndex").doesNotExist())
            .andExpect(jsonPath("$.weeks.length()").value(CampaignSchedule.WEEK_COUNT));
    }

    @Test
    @DisplayName("Starts, replays, settles and closes the campaign through the production rollover")
    void shouldRunTheCampaignFromItsFirstMondayToItsClosing() throws Exception {
        Campaign campaign = lifecycleService.open();

        mutableClock.setInstant(START_TIME);
        weeklyRolloverService.rolloverIfNeeded();

        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
            .isEqualTo(CampaignStatus.RUNNING);
        assertThat(weeklyChallengeRepository.findAllByWeekStartAndCadenceOrderByIdAsc(
            FIRST_WEEK_START,
            ChallengeCadence.WEEKLY
        )).hasSize(5);
        assertThat(weeklyChallengeRepository.findByCadenceAndDay(ChallengeCadence.DAILY, FIRST_WEEK_START))
            .isPresent();
        assertThat(calibrationSource.forWeek(FIRST_WEEK_START).reference()).isEqualTo(campaign.getReference());
        assertThat(calibrationSource.forWeek(FIRST_WEEK_START).weekIndex()).isEqualTo(1);
        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId())).hasSize(1);
        assertThat(scoreRepository.findAllByWeekStartOrderByPositionAsc(FIRST_WEEK_START)).hasSize(3);

        playCompetitiveMatches(alpha, FIRST_WEEK_START.plusDays(1), 3);
        mutableClock.setInstant(Instant.parse("2026-07-22T12:00:00Z"));
        mockMvc.perform(post("/api/admin/campaigns/replay").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isNoContent());

        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId())).hasSize(3);
        List<CampaignPlayerDay> alphaDays = playerDayRepository
            .findAllByCampaignIdAndPlayerIdOrderByDayAsc(campaign.getId(), alpha.getId());
        assertThat(alphaDays).hasSize(1);
        assertThat(alphaDays.getFirst().getDay()).isEqualTo(FIRST_WEEK_START.plusDays(1));
        assertThat(alphaDays.getFirst().getDamage()).isPositive();
        assertThat(alphaDays.getFirst().getMatchCount()).isEqualTo(3);
        assertThat(playerDayRepository.findAllByCampaignIdAndPlayerIdOrderByDayAsc(campaign.getId(), bravo.getId()))
            .isEmpty();

        mockMvc.perform(get("/api/campaign/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.day").value("2026-07-22"));

        mutableClock.setInstant(CLOSING_TIME);
        weeklyRolloverService.rolloverIfNeeded();

        Campaign closed = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(CampaignStatus.CLOSED);
        assertThat(closed.getClosedAt()).isEqualTo(CLOSING_TIME);
        assertThat(closed.getStoppedOn()).isNull();
        assertThat(closed.finalDay()).isEqualTo(FINAL_DAY);
        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId()))
            .hasSize(CampaignSchedule.WEEK_COUNT * 7);

        List<CampaignWeek> weeks = campaignWeekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());
        assertThat(weeks).allMatch(CampaignWeek::isSettled);
        assertThat(weeks.getFirst().getDamageDealt()).isEqualTo(alphaDays.getFirst().getDamage());
        assertThat(weeks.subList(1, weeks.size())).allMatch(week -> week.getDamageDealt() == 0);

        List<WeeklyPlayerScore> firstWeek = scoreRepository.findAllByWeekStartOrderByPositionAsc(FIRST_WEEK_START);
        assertThat(firstWeek).allSatisfy(score -> assertThat(score.getFinalizedAt()).isEqualTo(CLOSING_TIME));
        assertThat(firstWeek.getFirst().getPlayer().getId()).isEqualTo(alpha.getId());
        assertThat(firstWeek.getFirst().getGuardianDamage()).isEqualTo(alphaDays.getFirst().getDamage());

        // Between two campaigns the challenges keep the last closed campaign's reference.
        assertThat(calibrationSource.forWeek(LocalDate.of(2026, 9, 28)).reference())
            .isEqualTo(campaign.getReference());

        mockMvc.perform(get("/api/campaign"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.number").value(1));
        mockMvc.perform(get("/api/campaign/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].number").value(1))
            .andExpect(jsonPath("$[0].weeklyPopulation.length()").value(CampaignSchedule.WEEK_COUNT));
    }

    @Test
    @DisplayName("Stops a campaign early and deletes it with everything it owns")
    void shouldStopAndDeleteACampaignFromTheBackoffice() throws Exception {
        Campaign campaign = lifecycleService.open();
        mutableClock.setInstant(Instant.parse("2026-07-29T12:00:00Z"));
        lifecycleService.startIfDue();
        mockMvc.perform(post("/api/admin/campaigns/replay").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isNoContent());
        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId())).hasSize(10);

        mockMvc.perform(post("/api/admin/campaigns/stop").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.stoppedOn").value("2026-07-28"));

        mockMvc.perform(post("/api/admin/campaigns/stop").header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isConflict());

        Campaign next = lifecycleService.open();
        assertThat(next.getNumber()).isEqualTo(2);
        assertThat(next.getFirstWeekStart()).isEqualTo(LocalDate.of(2026, 8, 3));

        // Production deletes from a fresh persistence context; the roster rows saved above must not linger.
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(delete("/api/admin/campaigns/{id}", campaign.getId()).header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/campaigns/{id}", campaign.getId()).header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isNotFound());

        // The database cascade only runs once the delete is flushed.
        entityManager.flush();
        entityManager.clear();
        assertThat(campaignRepository.findById(campaign.getId())).isEmpty();
        assertThat(campaignWeekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId())).isEmpty();
        assertThat(campaignPlayerRepository.findAllByCampaignIdOrderByPlayerIdAsc(campaign.getId())).isEmpty();
        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId())).isEmpty();
        assertThat(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).map(Campaign::getId)
            .contains(next.getId());
    }

    private static List<LocalDate> weekStarts() {
        return CampaignSchedule.weeks().stream()
            .map(shape -> FIRST_WEEK_START.plusWeeks(shape.weekIndex() - 1L))
            .toList();
    }

    private Player createPlayer(String riotPuuid, String gameName, PlayerStatus status) {
        Player player = new Player();
        player.setRiotPuuid(riotPuuid);
        player.setGameName(gameName);
        player.setTagLine("TEST");
        player.setDisplayName(gameName + "#TEST");
        player.setPortrait("default");
        player.setStatus(status);
        return playerRepository.save(player);
    }

    private Season createSeason() {
        Season created = new Season();
        created.setExternalId("campaign-lifecycle-season");
        created.setName("Campaign Lifecycle Season");
        created.setActive(true);
        return seasonRepository.save(created);
    }

    /**
     * Plays competitive wins on one day, one hour apart.
     */
    private void playCompetitiveMatches(Player player, LocalDate day, int played) {
        for (int index = 0; index < played; index++) {
            Instant startedAt = day.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3_600L * (index + 10));

            ValorantMatch match = new ValorantMatch();
            match.setExternalMatchId("lifecycle-" + player.getGameName() + "-" + day + "-" + index);
            match.setSeason(season);
            match.setStartedAt(startedAt);
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
            playerMatch.setTeamId("Red");
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
            playerMatchRepository.save(playerMatch);
        }
    }

    /**
     * Supplies a mutable UTC clock so one test walks the campaign's ten weeks.
     */
    @TestConfiguration
    static class MutableClockConfiguration {

        /**
         * Creates the mutable primary application clock.
         */
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(OPENING_TIME, ZoneOffset.UTC);
        }
    }

    /**
     * Clock whose instant the test moves forward.
     */
    static final class MutableClock extends Clock {

        /**
         * Current clock instant.
         */
        private final AtomicReference<Instant> instant;

        /**
         * Clock zone.
         */
        private final ZoneId zone;

        /**
         * Creates a mutable clock.
         */
        private MutableClock(Instant initialInstant, ZoneId zone) {
            this.instant = new AtomicReference<>(initialInstant);
            this.zone = zone;
        }

        /**
         * Updates the current clock instant.
         */
        void setInstant(Instant newInstant) {
            instant.set(newInstant);
        }

        /**
         * Returns the configured zone.
         */
        @Override
        public ZoneId getZone() {
            return zone;
        }

        /**
         * Returns a clock reading the same instant in another zone.
         */
        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(instant.get(), newZone);
        }

        /**
         * Returns the current mutable instant.
         */
        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
