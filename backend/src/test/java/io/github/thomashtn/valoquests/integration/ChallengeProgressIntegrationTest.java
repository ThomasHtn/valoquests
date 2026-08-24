package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the complete weekly challenge-progress pipeline against PostgreSQL.
 *
 * <p>The test persists real players, matches, challenge definitions and
 * weekly selections. It then executes the production recalculation service
 * and verifies progress persistence and ranking generation.</p>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Import(
    ChallengeProgressIntegrationTest.FixedClockConfiguration.class
)
@Transactional
class ChallengeProgressIntegrationTest
    extends PostgreSqlIntegrationTest {

    /**
     * Monday identifying the deterministic test week.
     */
    private static final LocalDate WEEK_START =
        LocalDate.of(2026, 7, 20);

    /**
     * Fixed calculation time used by every time-dependent service.
     */
    private static final Instant CALCULATION_TIME =
        Instant.parse("2026-07-24T12:00:00Z");

    /**
     * Player repository used to prepare the test participant.
     */
    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Season repository used to prepare persisted match metadata.
     */
    @Autowired
    private SeasonRepository seasonRepository;

    /**
     * Match repository used to persist shared match data.
     */
    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    /**
     * Player-match repository used to persist individual statistics.
     */
    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Challenge catalogue repository used to create deterministic rules.
     */
    @Autowired
    private ChallengeRepository challengeRepository;

    /**
     * Weekly challenge repository used to prepare the active pack.
     */
    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Progress repository used to inspect calculated results.
     */
    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Score repository used to inspect the generated ranking.
     */
    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Production service under test.
     */
    @Autowired
    private ChallengeRecalculationService challengeRecalculationService;

    /**
     * Verifies metric aggregation, match filtering, persistence and ranking.
     */
    @Test
    void shouldCalculateWeeklyProgressAndRankingFromPersistedMatches() {
        deactivateSeededPlayers();

        Player player = createActivePlayer();
        Season season = createSeason();

        createCompetitiveMatch(
            player,
            season,
            "integration-match-1",
            "2026-07-20T18:00:00Z",
            MatchResult.WIN,
            20,
            10,
            5,
            2_000,
            "Omen"
        );

        createCompetitiveMatch(
            player,
            season,
            "integration-match-2",
            "2026-07-21T18:00:00Z",
            MatchResult.LOSS,
            15,
            10,
            7,
            1_500,
            "Sova"
        );

        createCompetitiveMatch(
            player,
            season,
            "integration-match-3",
            "2026-07-22T18:00:00Z",
            MatchResult.WIN,
            10,
            5,
            4,
            1_800,
            "Neon"
        );

        createCompetitiveMatch(
            player,
            season,
            "integration-match-4",
            "2026-07-23T18:00:00Z",
            MatchResult.LOSS,
            5,
            5,
            3,
            700,
            "Killjoy"
        );

        createExcludedMatch(
            player,
            season,
            "integration-deathmatch",
            "2026-07-24T10:00:00Z",
            GameMode.DEATHMATCH,
            40
        );

        // Clearly before the week's start under the Europe/Paris zone (Monday 00:00 Paris =
        // 2026-07-19T22:00:00Z): a timestamp equal to that boundary would fall inside the week instead
        // of before it, since the period start is inclusive.
        createExcludedMatch(
            player,
            season,
            "integration-previous-week",
            "2026-07-19T20:00:00Z",
            GameMode.COMPETITIVE,
            100
        );

        createWeeklyChallengePack();

        challengeRecalculationService
            .recalculateCurrentWeekProgress();

        Map<String, PlayerChallengeProgress> progressByCode =
            loadProgressByChallengeCode();

        assertThat(progressByCode).hasSize(5);

        assertProgress(
            progressByCode.get("INTEGRATION_KILLS"),
            "50.0000",
            "50.0000"
        );

        assertProgress(
            progressByCode.get("INTEGRATION_DAMAGE"),
            "6000.0000",
            "6000.0000"
        );

        assertProgress(
            progressByCode.get("INTEGRATION_WINS"),
            "2.0000",
            "2.0000"
        );

        assertProgress(
            progressByCode.get("INTEGRATION_KD"),
            "1.6667",
            "1.5000"
        );

        assertProgress(
            progressByCode.get("INTEGRATION_PLAY_DAYS"),
            "4.0000",
            "4.0000"
        );

        assertGeneratedRanking(player);
    }

    /**
     * Loads calculated progress indexed by stable challenge code.
     *
     * @return progress rows indexed by challenge code
     */
    private Map<String, PlayerChallengeProgress> loadProgressByChallengeCode() {
        return progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                WEEK_START
            )
            .stream()
            .collect(
                Collectors.toMap(
                    progress -> progress
                        .getWeeklyChallenge()
                        .getChallenge()
                        .getCode(),
                    Function.identity()
                )
            );
    }

    /**
     * Verifies the ranking generated from completed challenge progress.
     *
     * <p>Challenge damage is resolved from {@code DefaultScoringRuleset} by difficulty tier: completing
     * all five (EASY 800 + NORMAL 1400 + MEDIUM 2200 + HARD 3200 + VERY_HARD 4500) totals 12100. Match
     * damage sums the five valued matches this player played this week — four COMPETITIVE (WIN 500 +
     * LOSS 350 + WIN 500 + LOSS 350) plus the Deathmatch match, which reaches the 40-kill victory
     * threshold (WIN 150) — for 1850, none of them reaching the sixth match of its day. Those five
     * matches also span five distinct calendar days (the Deathmatch match falls on its own day), so the
     * regularity bonus is the 5-day tier, 3600. A single active player means no challenge here is
     * shared, so the team bonus stays at zero.
     *
     * @param player expected ranked player
     */
    private void assertGeneratedRanking(Player player) {
        List<WeeklyPlayerScore> scores =
            scoreRepository
                .findAllByWeekStartOrderByPositionAsc(
                    WEEK_START
                );

        assertThat(scores)
            .singleElement()
            .satisfies(score -> {
                assertThat(score.getPlayer().getId())
                    .isEqualTo(player.getId());

                assertThat(score.getPosition())
                    .isEqualTo(1);

                assertThat(score.getPreviousPosition())
                    .isNull();

                assertThat(score.getChallengeDamage())
                    .isEqualTo(12_100);

                assertThat(score.getCompletedChallenges())
                    .isEqualTo(5);

                assertThat(score.getMatchDamage())
                    .isEqualTo(1_850);

                assertThat(score.getActiveDays())
                    .isEqualTo(5);

                assertThat(score.getRegularityBonus())
                    .isEqualTo(3_600);

                assertThat(score.getTeamBonus())
                    .isEqualTo(0);

                assertThat(score.getTotalDamage())
                    .isEqualTo(17_550);

                assertThat(score.getCalculatedAt())
                    .isEqualTo(CALCULATION_TIME);
            });
    }

    /**
     * Removes Flyway-seeded players so only the test player is ranked. Marking them inactive is
     * not enough: an inactive player still gets a weekly score built for display, so they would
     * still show up in the ranking assertions below.
     */
    private void deactivateSeededPlayers() {
        playerRepository.deleteAll();
    }

    /**
     * Creates the only active player evaluated by the test.
     *
     * @return persisted active player
     */
    private Player createActivePlayer() {
        Player player = new Player();

        player.setRiotPuuid(
            "integration-player-puuid"
        );
        player.setGameName(
            "IntegrationPlayer"
        );
        player.setTagLine(
            "TEST"
        );
        player.setDisplayName(
            "IntegrationPlayer#TEST"
        );
        player.setPortrait(
            "default"
        );
        player.setStatus(
            PlayerStatus.ACTIVE
        );

        return playerRepository.save(player);
    }

    /**
     * Creates the season referenced by every integration-test match.
     *
     * @return persisted season
     */
    private Season createSeason() {
        Season season = new Season();

        season.setExternalId(
            "integration-season"
        );
        season.setName(
            "Integration Season"
        );
        season.setStartsAt(
            Instant.parse(
                "2026-07-01T00:00:00Z"
            )
        );
        season.setEndsAt(
            Instant.parse(
                "2026-08-31T23:59:59Z"
            )
        );
        season.setActive(true);

        return seasonRepository.save(season);
    }

    /**
     * Creates one competitive match with complete player statistics.
     *
     * @param player          tracked player
     * @param season          match season
     * @param externalMatchId external match identifier
     * @param startedAt       match start timestamp
     * @param result          player match result
     * @param kills           player kills
     * @param deaths          player deaths
     * @param assists         player assists
     * @param damageDealt     player damage
     * @param agentName       selected agent
     */
    private void createCompetitiveMatch(
        Player player,
        Season season,
        String externalMatchId,
        String startedAt,
        MatchResult result,
        int kills,
        int deaths,
        int assists,
        int damageDealt,
        String agentName
    ) {
        ValorantMatch match = createMatch(
            season,
            externalMatchId,
            startedAt,
            GameMode.COMPETITIVE
        );

        PlayerMatch playerMatch = createPlayerMatch(
            player,
            match,
            result,
            kills,
            deaths,
            assists,
            damageDealt,
            agentName
        );

        playerMatchRepository.save(playerMatch);
    }

    /**
     * Creates a match that must be excluded by mode or weekly-period filters.
     *
     * @param player          tracked player
     * @param season          match season
     * @param externalMatchId external match identifier
     * @param startedAt       match start timestamp
     * @param gameMode        match mode
     * @param kills           player kills
     */
    private void createExcludedMatch(
        Player player,
        Season season,
        String externalMatchId,
        String startedAt,
        GameMode gameMode,
        int kills
    ) {
        ValorantMatch match = createMatch(
            season,
            externalMatchId,
            startedAt,
            gameMode
        );

        PlayerMatch playerMatch = createPlayerMatch(
            player,
            match,
            MatchResult.WIN,
            kills,
            1,
            0,
            10_000,
            "Phoenix"
        );

        playerMatchRepository.save(playerMatch);
    }

    /**
     * Creates and persists shared match metadata.
     *
     * @param season          match season
     * @param externalMatchId external match identifier
     * @param startedAt       match start timestamp
     * @param gameMode        normalized game mode
     * @return persisted match
     */
    private ValorantMatch createMatch(
        Season season,
        String externalMatchId,
        String startedAt,
        GameMode gameMode
    ) {
        ValorantMatch match = new ValorantMatch();

        match.setExternalMatchId(externalMatchId);
        match.setSeason(season);
        match.setStartedAt(
            Instant.parse(startedAt)
        );
        match.setDurationSeconds(2_400);
        match.setMapId("integration-map");
        match.setMapName("Ascent");
        match.setGameMode(gameMode);
        match.setGameModeSource(GameModeSource.PROVIDED);
        match.setQueueId(
            gameMode.name().toLowerCase()
        );
        match.setRedScore(13);
        match.setBlueScore(10);

        return valorantMatchRepository.save(match);
    }

    /**
     * Creates complete statistics for one player and one match.
     *
     * @param player      tracked player
     * @param match       associated match
     * @param result      match result
     * @param kills       kills
     * @param deaths      deaths
     * @param assists     assists
     * @param damageDealt damage dealt
     * @param agentName   selected agent
     * @return unsaved player-match entity
     */
    private PlayerMatch createPlayerMatch(
        Player player,
        ValorantMatch match,
        MatchResult result,
        int kills,
        int deaths,
        int assists,
        int damageDealt,
        String agentName
    ) {
        PlayerMatch playerMatch = new PlayerMatch();

        playerMatch.setPlayer(player);
        playerMatch.setMatch(match);
        playerMatch.setTeamId("Blue");
        playerMatch.setAgentId(
            agentName.toLowerCase()
        );
        playerMatch.setAgentName(agentName);
        playerMatch.setResult(result);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(deaths);
        playerMatch.setAssists(assists);
        playerMatch.setScore(kills * 250);
        playerMatch.setHeadshots(kills / 2);
        playerMatch.setBodyshots(kills);
        playerMatch.setLegshots(0);
        playerMatch.setDamageDealt(damageDealt);
        playerMatch.setRoundsPlayed(23);
        playerMatch.setAcs(
            BigDecimal.valueOf(kills * 10L)
        );
        playerMatch.setAdr(
            BigDecimal.valueOf(damageDealt)
                .divide(
                    BigDecimal.valueOf(23),
                    2,
                    RoundingMode.HALF_UP
                )
        );
        playerMatch.setMvp(false);

        return playerMatch;
    }

    /**
     * Creates one deterministic challenge for every supported difficulty.
     */
    private void createWeeklyChallengePack() {
        List<Challenge> challenges =
            challengeRepository.saveAll(
                List.of(
                    createKillsChallenge(),
                    createDamageChallenge(),
                    createWinsChallenge(),
                    createKillDeathChallenge(),
                    createPlayDaysChallenge()
                )
            );

        List<WeeklyChallenge> weeklyChallenges =
            challenges.stream()
                .map(this::createWeeklyChallenge)
                .toList();

        weeklyChallengeRepository.saveAll(
            weeklyChallenges
        );
    }

    /**
     * Creates the weekly kills challenge.
     *
     * @return configured challenge
     */
    private Challenge createKillsChallenge() {
        return createChallenge(
            "INTEGRATION_KILLS",
            ChallengeDifficulty.EASY,
            ProgressMode.SUM,
            """
                [
                  {
                    "metric": "KILLS",
                    "operator": "GTE",
                    "target": 50,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );
    }

    /**
     * Creates the weekly damage challenge.
     *
     * @return configured challenge
     */
    private Challenge createDamageChallenge() {
        return createChallenge(
            "INTEGRATION_DAMAGE",
            ChallengeDifficulty.NORMAL,
            ProgressMode.SUM,
            """
                [
                  {
                    "metric": "DAMAGE_DEALT",
                    "operator": "GTE",
                    "target": 6000,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );
    }

    /**
     * Creates the weekly wins challenge.
     *
     * @return configured challenge
     */
    private Challenge createWinsChallenge() {
        return createChallenge(
            "INTEGRATION_WINS",
            ChallengeDifficulty.MEDIUM,
            ProgressMode.SUM,
            """
                [
                  {
                    "metric": "MATCHES_WON",
                    "operator": "GTE",
                    "target": 2,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );
    }

    /**
     * Creates the weekly kill/death-ratio challenge.
     *
     * @return configured challenge
     */
    private Challenge createKillDeathChallenge() {
        return createChallenge(
            "INTEGRATION_KD",
            ChallengeDifficulty.HARD,
            ProgressMode.RATIO,
            """
                [
                  {
                    "metric": "KD",
                    "operator": "GTE",
                    "target": 1.5,
                    "gameMode": "COMPETITIVE",
                    "minimumMatches": 4
                  }
                ]
                """
        );
    }

    /**
     * Creates the weekly distinct play-days challenge.
     *
     * @return configured challenge
     */
    private Challenge createPlayDaysChallenge() {
        return createChallenge(
            "INTEGRATION_PLAY_DAYS",
            ChallengeDifficulty.VERY_HARD,
            ProgressMode.DISTINCT_COUNT,
            """
                [
                  {
                    "metric": "PLAY_DAY",
                    "operator": "GTE",
                    "target": 4,
                    "gameMode": "COMPETITIVE",
                    "groupBy": "PLAY_DAY"
                  }
                ]
                """
        );
    }

    /**
     * Creates one complete challenge catalogue entry.
     *
     * @param code           stable challenge code
     * @param difficulty     challenge difficulty
     * @param progressMode   calculation mode
     * @param conditionsJson serialized rule definition
     * @return configured unsaved challenge
     */
    private Challenge createChallenge(
        String code,
        ChallengeDifficulty difficulty,
        ProgressMode progressMode,
        String conditionsJson
    ) {
        Challenge challenge = new Challenge();

        challenge.setCode(code);
        challenge.setName(code);
        challenge.setDescription(
            "Integration challenge " + code
        );
        challenge.setDifficulty(difficulty);
        challenge.setCategory(
            ChallengeCategory.OTHER
        );
        challenge.setProgressMode(progressMode);
        challenge.setConditionsJson(
            conditionsJson
        );
        challenge.setEnabled(true);
        challenge.setSchemaVersion(3);

        return challenge;
    }

    /**
     * Associates one catalogue challenge with the deterministic test week.
     *
     * @param challenge persisted catalogue challenge
     * @return unsaved weekly selection
     */
    private WeeklyChallenge createWeeklyChallenge(
        Challenge challenge
    ) {
        WeeklyChallenge weeklyChallenge =
            new WeeklyChallenge();

        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(
            CALCULATION_TIME.minusSeconds(3_600)
        );

        return weeklyChallenge;
    }

    /**
     * Verifies one persisted challenge-progress row.
     *
     * @param progress             persisted progress
     * @param expectedCurrentValue expected current value
     * @param expectedTargetValue  expected target value
     */
    private void assertProgress(
        PlayerChallengeProgress progress,
        String expectedCurrentValue,
        String expectedTargetValue
    ) {
        assertThat(progress).isNotNull();

        assertThat(progress.getCurrentValue())
            .isEqualByComparingTo(
                expectedCurrentValue
            );

        assertThat(progress.getTargetValue())
            .isEqualByComparingTo(
                expectedTargetValue
            );

        assertThat(progress.isCompleted())
            .isTrue();

        assertThat(progress.getCompletedAt())
            .isEqualTo(CALCULATION_TIME);

        assertThat(progress.getCalculatedAt())
            .isEqualTo(CALCULATION_TIME);
    }

    /**
     * Overrides the production clock with a deterministic UTC clock.
     */
    @TestConfiguration
    static class FixedClockConfiguration {

        /**
         * Provides the clock used by recalculation and persistence services.
         *
         * @return fixed UTC clock
         */
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                CALCULATION_TIME,
                ZoneOffset.UTC
            );
        }
    }
}
