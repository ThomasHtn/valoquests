package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.colony.dto.ColonyResponse;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyGauge;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.colony.service.ColonyQueryService;
import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
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
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.repository.RunRepository;
import io.github.thomashtn.valoquests.run.service.RunService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies a run's colony end to end, against a real database.
 *
 * <p>Seeds a run, a squad, their matches, a week of completed challenges and a defeated boss, then
 * replays the colony through the production services and reads it back through the query service.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false",
        "app.scheduling.colony-tick-enabled=false"
    }
)
@Import(ColonyRunIntegrationTest.MutableClockConfiguration.class)
@Transactional
class ColonyRunIntegrationTest extends PostgreSqlIntegrationTest {

    /** Monday the run opens on. */
    private static final LocalDate FIRST_WEEK = LocalDate.of(2026, 7, 13);

    /** Instant the run's first Monday starts at. */
    private static final Instant RUN_START = Instant.parse("2026-07-13T00:15:00Z");

    /** Roster the run is measured against. */
    private static final int ROSTER_SIZE = 3;

    /** Player repository. */
    @Autowired
    private PlayerRepository playerRepository;

    /** Season repository. */
    @Autowired
    private SeasonRepository seasonRepository;

    /** Match repository. */
    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    /** Player-match repository. */
    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    /** Challenge catalogue repository. */
    @Autowired
    private ChallengeRepository challengeRepository;

    /** Weekly challenge repository. */
    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    /** Challenge progress repository. */
    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    /** Boss catalogue repository. */
    @Autowired
    private BossCatalogEntryRepository bossCatalogEntryRepository;

    /** Boss encounter repository. */
    @Autowired
    private WeeklyBossEncounterRepository encounterRepository;

    /** Snapshot repository. */
    @Autowired
    private ColonyDailySnapshotRepository snapshotRepository;

    /** Run repository, used to clear any run another test left behind. */
    @Autowired
    private RunRepository runRepository;

    /** Run service. */
    @Autowired
    private RunService runService;

    /** Replay service under test. */
    @Autowired
    private ColonyReplayService replayService;

    /** Query service under test. */
    @Autowired
    private ColonyQueryService queryService;

    /** Clock the test advances through the run. */
    @Autowired
    private MutableClock mutableClock;

    /**
     * Verifies a week of play end to end: one snapshot a day, the rollover's materials on the eighth,
     * and the capacity they unlock.
     */
    @Test
    void shouldBuildTheColonyFromAWeekOfPlay() {
        Run run = seedRunAndWeekOfPlay();

        // Monday of the second week: the rollover has closed week one and credits its materials.
        mutableClock.setInstant(RUN_START.plus(java.time.Duration.ofDays(7)));
        replayService.replayCurrentRun();

        List<ColonyDailySnapshot> snapshots =
            snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());

        assertThat(snapshots).hasSize(8);
        assertThat(snapshots.getFirst().getDay()).isEqualTo(FIRST_WEEK);
        assertThat(snapshots.getLast().getDay()).isEqualTo(FIRST_WEEK.plusDays(7));

        // Six days without a rollover carry no materials at all.
        assertThat(snapshots.subList(0, 7))
            .allSatisfy(snapshot -> assertThat(snapshot.getMaterials()).isZero());

        // Day eight credits three players' HARD challenge, at 32 each, plus 400 for the boss.
        assertThat(snapshots.getLast().getMaterials()).isEqualTo(3 * 32 + 400);
        assertThat(snapshots.getLast().getCapacity()).isEqualTo(3_000);
    }

    /**
     * Verifies that the colony is a pure function of its inputs: two consecutive replays write
     * strictly identical rows.
     */
    @Test
    void shouldBeIdempotentAcrossConsecutiveReplays() {
        Run run = seedRunAndWeekOfPlay();
        mutableClock.setInstant(RUN_START.plus(java.time.Duration.ofDays(7)));

        replayService.replayCurrentRun();
        List<SnapshotValues> first = readValues(run);

        replayService.replayCurrentRun();
        List<SnapshotValues> second = readValues(run);

        assertThat(second).isEqualTo(first);
    }

    /**
     * Verifies that archiving a player mid-run changes no day already computed.
     *
     * <p>Two things make this hold. The roster size is frozen on the run, so the Energy denominator
     * cannot move; and the gauges count every player's matches whatever their status, so a player
     * leaving the roster does not retroactively erase the games they played.
     */
    @Test
    void shouldNotRewriteHistoryWhenAPlayerIsArchived() {
        Run run = seedRunAndWeekOfPlay();
        mutableClock.setInstant(RUN_START.plus(java.time.Duration.ofDays(7)));

        replayService.replayCurrentRun();
        List<SnapshotValues> before = readValues(run);

        Player archived = playerRepository.findAllByOrderByIdAsc().getFirst();
        archived.setStatus(PlayerStatus.ARCHIVED);
        playerRepository.saveAndFlush(archived);

        replayService.replayCurrentRun();

        assertThat(readValues(run)).isEqualTo(before);
    }

    /**
     * Verifies that the settlement day closes the run and carries its score.
     *
     * <p>The seventy-first day is what stops the tenth week from being the only one to bring nothing
     * in: it credits that week's materials and applies one last migration.
     */
    @Test
    void shouldCarryTheRunScoreOnItsSettlementDay() {
        Run run = seedRunAndWeekOfPlay();

        // Past the settlement day: the replay stops at it rather than running on.
        mutableClock.setInstant(RUN_START.plus(java.time.Duration.ofDays(90)));
        replayService.replay(run);

        List<ColonyDailySnapshot> snapshots =
            snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());

        assertThat(snapshots).hasSize(71);
        assertThat(snapshots.getLast().getDay()).isEqualTo(run.settlementDay());
        assertThat(snapshots.getLast().getDay()).isEqualTo(FIRST_WEEK.plusDays(70));
    }

    /**
     * Verifies that the query service reads the colony back with its run position, its buildings and
     * the gauge that is limiting it.
     */
    @Test
    void shouldReadTheColonyBackThroughTheApi() {
        seedRunAndWeekOfPlay();
        mutableClock.setInstant(RUN_START.plus(java.time.Duration.ofDays(7)));

        ColonyResponse colony = queryService.findCurrent();

        assertThat(colony.runNumber()).isEqualTo(1);
        assertThat(colony.runDay()).isEqualTo(8);
        assertThat(colony.runDayCount()).isEqualTo(71);
        assertThat(colony.runWeekIndex()).isEqualTo(2);
        assertThat(colony.materials()).isEqualTo(3 * 32 + 400);
        assertThat(colony.capacity()).isEqualTo(3_000);
        assertThat(colony.maximumCapacity()).isEqualTo(7_000);
        assertThat(colony.defeatedBosses()).isEqualTo(1);
        assertThat(colony.bossCount()).isEqualTo(10);
        assertThat(colony.buildings()).hasSize(4);
        assertThat(colony.nextTier().materialsThreshold()).isEqualTo(2_500);
        assertThat(colony.population()).isPositive();

        // Three players out of three turned up on the last day, so Energy is saturated and Food, fed
        // by a single match each, is what holds the colony back.
        assertThat(colony.limitingGauge()).isEqualTo(ColonyGauge.FOOD);
    }

    /**
     * Seeds a run, three players, a week of daily matches, a completed challenge and a defeated boss.
     *
     * @return the run in progress
     */
    private Run seedRunAndWeekOfPlay() {
        mutableClock.setInstant(RUN_START);

        // Another integration test may have left an open run behind: reading the boss endpoint alone
        // opens one lazily. Clearing them is what lets this test own the run it measures.
        snapshotRepository.deleteAllInBatch();
        encounterRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();

        Season season = new Season();
        season.setExternalId("v26a4");
        season.setName("v26a4");
        season = seasonRepository.save(season);

        // The migrations seed the real roster. Archiving it leaves the run measured against this
        // test's own squad alone, which is what makes the Energy arithmetic below predictable.
        List<Player> seeded = playerRepository.findAllByOrderByIdAsc();
        seeded.forEach(player -> player.setStatus(PlayerStatus.ARCHIVED));
        playerRepository.saveAllAndFlush(seeded);

        List<Player> squad = List.of(
            persistPlayer("Alpha"),
            persistPlayer("Bravo"),
            persistPlayer("Charlie")
        );

        assertThat(playerRepository.countByStatus(PlayerStatus.ACTIVE))
            .describedAs("active roster before opening the run")
            .isEqualTo(ROSTER_SIZE);

        Run run = runService.ensureRunFor(FIRST_WEEK);
        assertThat(run.getRosterSize()).isEqualTo(ROSTER_SIZE);

        // One competitive win each, on every day of the first week.
        for (int day = 0; day < 7; day++) {
            for (Player player : squad) {
                persistWin(player, season, day);
            }
        }

        persistCompletedChallenge(squad);
        persistDefeatedBoss(run);

        return run;
    }

    /**
     * Persists one active player.
     *
     * @param name display name
     * @return the persisted player
     */
    private Player persistPlayer(String name) {
        Player player = new Player();
        player.setRiotPuuid("colony-" + name);
        player.setGameName(name);
        player.setTagLine("EUW");
        player.setDisplayName(name);
        player.setStatus(PlayerStatus.ACTIVE);

        return playerRepository.saveAndFlush(player);
    }

    /**
     * Persists one competitive win.
     *
     * @param player  player who played it
     * @param season  act it belongs to
     * @param dayIndex day of the run it was played on, from zero
     */
    private void persistWin(Player player, Season season, int dayIndex) {
        ValorantMatch match = new ValorantMatch();
        match.setExternalMatchId("colony-" + player.getGameName() + "-" + dayIndex);
        match.setSeason(season);
        match.setStartedAt(FIRST_WEEK.plusDays(dayIndex).atTime(20, 0).toInstant(ZoneOffset.UTC));
        match.setDurationSeconds(2_400);
        match.setMapId("ascent");
        match.setMapName("Ascent");
        match.setGameMode(GameMode.COMPETITIVE);
        match.setGameModeSource(GameModeSource.PROVIDED);
        match.setQueueId("competitive");
        match = valorantMatchRepository.save(match);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setPlayer(player);
        playerMatch.setMatch(match);
        playerMatch.setTeamId("Blue");
        playerMatch.setAgentId("omen");
        playerMatch.setAgentName("Omen");
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(20);
        playerMatch.setDeaths(12);
        playerMatch.setAssists(5);
        playerMatch.setScore(5_000);
        playerMatch.setHeadshots(10);
        playerMatch.setBodyshots(20);
        playerMatch.setLegshots(0);
        playerMatch.setDamageDealt(4_000);
        playerMatch.setRoundsPlayed(23);

        playerMatchRepository.save(playerMatch);
    }

    /**
     * Persists a HARD challenge the whole squad completed during the first week.
     *
     * @param squad the players
     */
    private void persistCompletedChallenge(List<Player> squad) {
        Challenge challenge = new Challenge();
        challenge.setCode("COLONY_HARD");
        challenge.setName("COLONY_HARD");
        challenge.setDescription("Colony integration challenge");
        challenge.setDifficulty(ChallengeDifficulty.HARD);
        challenge.setCategory(ChallengeCategory.OTHER);
        challenge.setProgressMode(ProgressMode.COUNT_MATCHES);
        challenge.setConditionsJson(
            """
                [
                  {
                    "metric": "MATCHES",
                    "operator": "GTE",
                    "target": 1,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );
        challenge.setEnabled(true);
        challenge.setSchemaVersion(3);
        challenge = challengeRepository.save(challenge);

        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(FIRST_WEEK);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(RUN_START);
        WeeklyChallenge persisted = weeklyChallengeRepository.save(weeklyChallenge);

        for (Player player : squad) {
            PlayerChallengeProgress progress = new PlayerChallengeProgress();
            progress.setPlayer(player);
            progress.setWeeklyChallenge(persisted);
            progress.setCurrentValue(BigDecimal.ONE);
            progress.setTargetValue(BigDecimal.ONE);
            progress.setCompleted(true);
            progress.setCompletedAt(RUN_START);
            progress.setCalculatedAt(RUN_START);
            progressRepository.save(progress);
        }
    }

    /**
     * Persists a finalized, defeated fight for the run's first week.
     *
     * @param run run the fight belongs to
     */
    private void persistDefeatedBoss(Run run) {
        BossCatalogEntry catalogEntry = bossCatalogEntryRepository.findAll().getFirst();

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(FIRST_WEEK);
        encounter.setRun(run);
        encounter.setBossCatalogEntry(catalogEntry);
        encounter.setEffectiveHp(10_000);
        encounter.setDamageDealt(12_000);
        encounter.setActivePlayerCount(ROSTER_SIZE);
        encounter.setDefeated(true);
        encounter.setFinalizedAt(RUN_START.plus(java.time.Duration.ofDays(7)));

        encounterRepository.save(encounter);
    }

    /**
     * Reads a run's snapshots as plain comparable values.
     *
     * @param run run to read
     * @return one value tuple per day, oldest first
     */
    private List<SnapshotValues> readValues(Run run) {
        return snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId()).stream()
            .map(snapshot -> new SnapshotValues(
                snapshot.getDay(),
                snapshot.getFood(),
                snapshot.getEnergy(),
                snapshot.getPopulation(),
                snapshot.getMaterials(),
                snapshot.getCapacity(),
                snapshot.getActivePlayerCount()
            ))
            .toList();
    }

    /**
     * One snapshot reduced to the values a replay must reproduce exactly.
     *
     * @param day               calendar day
     * @param food              Food gauge
     * @param energy            Energy gauge
     * @param population        population
     * @param materials         cumulative materials
     * @param capacity          capacity
     * @param activePlayerCount distinct active players
     */
    private record SnapshotValues(
        LocalDate day,
        BigDecimal food,
        BigDecimal energy,
        BigDecimal population,
        int materials,
        int capacity,
        int activePlayerCount
    ) {
    }

    /**
     * Registers the mutable application clock.
     */
    @TestConfiguration
    static class MutableClockConfiguration {

        /**
         * Creates the mutable primary application clock.
         *
         * @return the clock
         */
        @Bean
        @Primary
        MutableClock colonyMutableClock() {
            return new MutableClock(RUN_START, ZoneOffset.UTC);
        }
    }

    /**
     * Clock whose instant the test can advance.
     */
    static final class MutableClock extends Clock {

        /** Current clock instant. */
        private final AtomicReference<Instant> instant;

        /** Clock zone. */
        private final ZoneId zone;

        /**
         * Creates a mutable clock.
         *
         * @param initialInstant instant the clock starts at
         * @param zone           clock zone
         */
        MutableClock(Instant initialInstant, ZoneId zone) {
            this.instant = new AtomicReference<>(initialInstant);
            this.zone = zone;
        }

        /**
         * Moves the clock to an instant.
         *
         * @param newInstant instant to move to
         */
        void setInstant(Instant newInstant) {
            instant.set(newInstant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            MutableClock copy = new MutableClock(instant.get(), newZone);
            return copy;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
