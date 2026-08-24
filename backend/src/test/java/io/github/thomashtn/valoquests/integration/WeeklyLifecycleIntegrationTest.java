package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeRuleType;
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
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
 * Verifies the complete business lifecycle of a weekly competition.
 *
 * <p>The scenario starts from persisted players and matches. Production
 * services calculate challenge progress and ranking, then the application
 * clock advances to the next week before the real rollover service finalizes
 * the previous results and creates the next challenge pack.</p>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Import(WeeklyLifecycleIntegrationTest.MutableClockConfiguration.class)
@Transactional
class WeeklyLifecycleIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Monday identifying the competition week built by the test.
     */
    private static final LocalDate COMPETITION_WEEK_START =
        LocalDate.of(2026, 7, 13);

    /**
     * Monday identifying the week created by rollover.
     */
    private static final LocalDate NEXT_WEEK_START =
        COMPETITION_WEEK_START.plusWeeks(1);

    /**
     * Time at which weekly progress is calculated.
     */
    private static final Instant CALCULATION_TIME =
        Instant.parse("2026-07-19T20:00:00Z");

    /**
     * Time at which the next weekly lifecycle begins.
     */
    private static final Instant ROLLOVER_TIME =
        Instant.parse("2026-07-20T00:05:00Z");

    /**
     * Player repository used to prepare lifecycle participants.
     */
    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Season repository used to attach persisted matches.
     */
    @Autowired
    private SeasonRepository seasonRepository;

    /**
     * Match repository used to persist shared match metadata.
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
     * Weekly challenge repository used to inspect both weekly packs.
     */
    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Progress repository used to verify production calculations.
     */
    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Score repository used to verify ranking and finalization.
     */
    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Production challenge recalculation service.
     */
    @Autowired
    private ChallengeRecalculationService challengeRecalculationService;

    /**
     * Production weekly rollover service.
     */
    @Autowired
    private WeeklyRolloverService weeklyRolloverService;

    /**
     * Mutable application clock used to cross the weekly boundary.
     */
    @Autowired
    private MutableClock mutableClock;

    /**
     * Verifies matches, calculations, ranking, finalization and next-week
     * creation through the complete production workflow.
     */
    @Test
    void shouldRunCompleteWeeklyLifecycleFromMatchesToRollover() {
        mutableClock.setInstant(CALCULATION_TIME);
        deactivateSeededPlayers();

        Player alpha = createPlayer("lifecycle-alpha", "Alpha");
        Player bravo = createPlayer("lifecycle-bravo", "Bravo");
        Season season = createSeason();

        createAlphaMatches(alpha, season);
        createBravoMatches(bravo, season);
        createCompetitionChallengePack();

        challengeRecalculationService.recalculateCurrentWeekProgress();

        assertCalculatedProgress(alpha, bravo);
        assertCurrentRanking(alpha, bravo);

        mutableClock.setInstant(ROLLOVER_TIME);
        weeklyRolloverService.rolloverIfNeeded();

        assertPreviousWeekFinalized(alpha, bravo);
        assertNextWeekCreated();
    }

    /**
     * Creates matches that complete every deterministic challenge.
     */
    private void createAlphaMatches(Player player, Season season) {
        createCompetitiveMatch(player, season, "alpha-1", "2026-07-13T18:00:00Z", MatchResult.WIN, 20, 10, 5, 2_000);
        createCompetitiveMatch(player, season, "alpha-2", "2026-07-14T18:00:00Z", MatchResult.LOSS, 15, 10, 7, 1_500);
        createCompetitiveMatch(player, season, "alpha-3", "2026-07-15T18:00:00Z", MatchResult.WIN, 10, 5, 4, 1_800);
        createCompetitiveMatch(player, season, "alpha-4", "2026-07-16T18:00:00Z", MatchResult.LOSS, 5, 5, 3, 700);
    }

    /**
     * Creates weaker matches that complete only the kills challenge.
     */
    private void createBravoMatches(Player player, Season season) {
        createCompetitiveMatch(player, season, "bravo-1", "2026-07-13T20:00:00Z", MatchResult.LOSS, 15, 20, 3, 1_000);
        createCompetitiveMatch(player, season, "bravo-2", "2026-07-13T21:00:00Z", MatchResult.LOSS, 15, 20, 2, 1_000);
        createCompetitiveMatch(player, season, "bravo-3", "2026-07-14T20:00:00Z", MatchResult.WIN, 10, 15, 4, 900);
        createCompetitiveMatch(player, season, "bravo-4", "2026-07-14T21:00:00Z", MatchResult.LOSS, 10, 15, 1, 900);
    }

    /**
     * Verifies progress generated directly from persisted match statistics.
     */
    private void assertCalculatedProgress(Player alpha, Player bravo) {
        Map<Long, Map<String, PlayerChallengeProgress>> progressByPlayer =
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                    COMPETITION_WEEK_START
                )
                .stream()
                .collect(
                    Collectors.groupingBy(
                        progress -> progress.getPlayer().getId(),
                        Collectors.toMap(
                            progress -> progress.getWeeklyChallenge().getChallenge().getCode(),
                            Function.identity()
                        )
                    )
                );

        assertThat(progressByPlayer).containsOnlyKeys(alpha.getId(), bravo.getId());

        Map<String, PlayerChallengeProgress> alphaProgress = progressByPlayer.get(alpha.getId());
        assertThat(alphaProgress).hasSize(5);
        assertCompleted(alphaProgress.get("LIFECYCLE_KILLS"), "50.0000");
        assertCompleted(alphaProgress.get("LIFECYCLE_DAMAGE"), "6000.0000");
        assertCompleted(alphaProgress.get("LIFECYCLE_WINS"), "2.0000");
        assertCompleted(alphaProgress.get("LIFECYCLE_KD"), "1.6667");
        assertCompleted(alphaProgress.get("LIFECYCLE_PLAY_DAYS"), "4.0000");

        Map<String, PlayerChallengeProgress> bravoProgress = progressByPlayer.get(bravo.getId());
        assertThat(bravoProgress).hasSize(5);
        assertCompleted(bravoProgress.get("LIFECYCLE_KILLS"), "50.0000");
        assertIncomplete(bravoProgress.get("LIFECYCLE_DAMAGE"), "3800.0000");
        assertIncomplete(bravoProgress.get("LIFECYCLE_WINS"), "1.0000");
        assertIncomplete(bravoProgress.get("LIFECYCLE_KD"), "0.7143");
        assertIncomplete(bravoProgress.get("LIFECYCLE_PLAY_DAYS"), "2.0000");
    }

    /**
     * Verifies the live ranking created by challenge recalculation.
     *
     * <p>Challenge damage is resolved from {@code ScoringRulesetV2} by difficulty tier. Alpha completes
     * all five (EASY 1500 + NORMAL 2500 + MEDIUM 4000 + HARD 6000 + VERY_HARD 9000 = 23000); bravo only
     * completes the EASY kills challenge (1500). That kills challenge is shared, and both players
     * complete it, so both receive the 2-player team bonus, 10% of the challenge's own damage (150).
     * Match damage sums each player's four COMPETITIVE matches (alpha: WIN+LOSS+WIN+LOSS =
     * 500+350+500+350 = 1700; bravo: LOSS+LOSS+WIN+LOSS = 350+350+500+350 = 1550), none of which reaches
     * the sixth match of its day, so no daily coefficient applies. The regularity bonus follows each
     * player's own distinct match days (alpha spans 4 days = 2400; bravo spans 2 days = 600, matching
     * their own PLAY_DAY progress values).
     */
    private void assertCurrentRanking(Player alpha, Player bravo) {
        List<WeeklyPlayerScore> scores = loadScores(COMPETITION_WEEK_START);

        assertThat(scores).hasSize(2);
        assertScore(scores.get(0), alpha, 23_000, 27_250, 5, 1, null, null);
        assertScore(scores.get(1), bravo, 1_500, 3_800, 1, 2, null, null);
    }

    /**
     * Verifies immutable challenge and ranking snapshots after rollover.
     */
    private void assertPreviousWeekFinalized(Player alpha, Player bravo) {
        List<WeeklyChallenge> challenges = loadChallenges(COMPETITION_WEEK_START);
        assertThat(challenges)
            .hasSize(5)
            .allSatisfy(challenge ->
                assertThat(challenge.getFinalizedAt()).isEqualTo(ROLLOVER_TIME)
            );

        List<WeeklyPlayerScore> scores = loadScores(COMPETITION_WEEK_START);
        assertThat(scores).hasSize(2);
        assertScore(scores.get(0), alpha, 23_000, 27_250, 5, 1, 1, ROLLOVER_TIME);
        assertScore(scores.get(1), bravo, 1_500, 3_800, 1, 2, 2, ROLLOVER_TIME);
    }

    /**
     * Verifies creation of one fresh five-difficulty challenge pack.
     */
    private void assertNextWeekCreated() {
        List<WeeklyChallenge> challenges = loadChallenges(NEXT_WEEK_START);

        assertThat(challenges)
            .hasSize(5)
            .allSatisfy(challenge -> {
                assertThat(challenge.getSelectedAt()).isEqualTo(ROLLOVER_TIME);
                assertThat(challenge.getFinalizedAt()).isNull();
            });

        assertThat(challenges)
            .extracting(challenge -> challenge.getChallenge().getDifficulty())
            .containsExactlyInAnyOrder(
                ChallengeDifficulty.EASY,
                ChallengeDifficulty.NORMAL,
                ChallengeDifficulty.MEDIUM,
                ChallengeDifficulty.HARD,
                ChallengeDifficulty.VERY_HARD
            );

        assertThat(loadScores(NEXT_WEEK_START)).isEmpty();
    }

    /**
     * Verifies a completed persisted progress row.
     */
    private void assertCompleted(PlayerChallengeProgress progress, String currentValue) {
        assertThat(progress).isNotNull();
        assertThat(progress.getCurrentValue()).isEqualByComparingTo(currentValue);
        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.getCompletedAt()).isEqualTo(CALCULATION_TIME);
        assertThat(progress.getCalculatedAt()).isEqualTo(CALCULATION_TIME);
    }

    /**
     * Verifies an incomplete persisted progress row.
     */
    private void assertIncomplete(PlayerChallengeProgress progress, String currentValue) {
        assertThat(progress).isNotNull();
        assertThat(progress.getCurrentValue()).isEqualByComparingTo(currentValue);
        assertThat(progress.isCompleted()).isFalse();
        assertThat(progress.getCompletedAt()).isNull();
        assertThat(progress.getCalculatedAt()).isEqualTo(CALCULATION_TIME);
    }

    /**
     * Verifies one live or finalized ranking row.
     */
    private void assertScore(
        WeeklyPlayerScore score,
        Player player,
        int challengeDamage,
        int totalDamage,
        int completedChallenges,
        int position,
        Integer previousPosition,
        Instant finalizedAt
    ) {
        assertThat(score.getPlayer().getId()).isEqualTo(player.getId());
        assertThat(score.getChallengeDamage()).isEqualTo(challengeDamage);
        assertThat(score.getTotalDamage()).isEqualTo(totalDamage);
        assertThat(score.getCompletedChallenges()).isEqualTo(completedChallenges);
        assertThat(score.getPosition()).isEqualTo(position);
        assertThat(score.getPreviousPosition()).isEqualTo(previousPosition);
        assertThat(score.getCalculatedAt()).isEqualTo(
            finalizedAt == null ? CALCULATION_TIME : ROLLOVER_TIME
        );
        assertThat(score.getFinalizedAt()).isEqualTo(finalizedAt);
    }

    /**
     * Removes migration-seeded players for deterministic ranking. Marking them inactive is not
     * enough: an inactive player still gets a weekly score built for display, so they would still
     * show up in the ranking assertions below.
     */
    private void deactivateSeededPlayers() {
        playerRepository.deleteAll();
    }

    /**
     * Creates one active tracked player.
     */
    private Player createPlayer(String riotPuuid, String gameName) {
        Player player = new Player();
        player.setRiotPuuid(riotPuuid);
        player.setGameName(gameName);
        player.setTagLine("TEST");
        player.setDisplayName(gameName + "#TEST");
        player.setPortrait("default");
        player.setStatus(PlayerStatus.ACTIVE);
        return playerRepository.save(player);
    }

    /**
     * Creates the season referenced by lifecycle matches.
     */
    private Season createSeason() {
        Season season = new Season();
        season.setExternalId("lifecycle-season");
        season.setName("Lifecycle Season");
        season.setStartsAt(Instant.parse("2026-07-01T00:00:00Z"));
        season.setEndsAt(Instant.parse("2026-08-31T23:59:59Z"));
        season.setActive(true);
        return seasonRepository.save(season);
    }

    /**
     * Persists one complete competitive match and player performance.
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
        int damageDealt
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setExternalMatchId("lifecycle-" + externalMatchId);
        match.setSeason(season);
        match.setStartedAt(Instant.parse(startedAt));
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
        playerMatch.setAgentId("omen");
        playerMatch.setAgentName("Omen");
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
        playerMatch.setAcs(BigDecimal.valueOf(kills * 10L));
        playerMatch.setAdr(
            BigDecimal.valueOf(damageDealt)
                .divide(BigDecimal.valueOf(23), 2, RoundingMode.HALF_UP)
        );
        playerMatch.setMvp(false);
        playerMatchRepository.save(playerMatch);
    }

    /**
     * Creates the deterministic five-challenge competition pack.
     */
    private void createCompetitionChallengePack() {
        List<Challenge> challenges = challengeRepository.saveAll(
            List.of(
                createChallenge("LIFECYCLE_KILLS", ChallengeDifficulty.EASY, ProgressMode.SUM,
                    ChallengeRuleType.SINGLE, "KILLS", "50", null),
                createChallenge("LIFECYCLE_DAMAGE", ChallengeDifficulty.NORMAL, ProgressMode.SUM,
                    ChallengeRuleType.SINGLE, "DAMAGE_DEALT", "6000", null),
                createChallenge("LIFECYCLE_WINS", ChallengeDifficulty.MEDIUM, ProgressMode.SUM,
                    ChallengeRuleType.SINGLE, "MATCHES_WON", "2", null),
                createChallenge("LIFECYCLE_KD", ChallengeDifficulty.HARD, ProgressMode.RATIO,
                    ChallengeRuleType.RATIO, "KD", "1.5", "\"minimumMatches\": 4,"),
                createChallenge("LIFECYCLE_PLAY_DAYS", ChallengeDifficulty.VERY_HARD,
                    ProgressMode.DISTINCT_COUNT, ChallengeRuleType.DISTINCT, "PLAY_DAY", "4",
                    "\"groupBy\": \"PLAY_DAY\",")
            )
        );

        weeklyChallengeRepository.saveAll(
            challenges.stream()
                .map(this::createWeeklyChallenge)
                .toList()
        );
    }

    /**
     * Creates one catalogue challenge with a single production rule.
     */
    private Challenge createChallenge(
        String code,
        ChallengeDifficulty difficulty,
        ProgressMode progressMode,
        ChallengeRuleType ruleType,
        String metric,
        String target,
        String additionalCondition
    ) {
        String additionalJson = additionalCondition == null ? "" : additionalCondition;
        Challenge challenge = new Challenge();
        challenge.setCode(code);
        challenge.setName(code);
        challenge.setDescription("Lifecycle challenge " + code);
        challenge.setDifficulty(difficulty);
        challenge.setCategory(ChallengeCategory.OTHER);
        challenge.setRuleType(ruleType);
        challenge.setProgressMode(progressMode);
        challenge.setConditionsJson(
            """
                [
                  {
                    "metric": "%s",
                    "operator": "GTE",
                    "target": %s,
                    %s
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """.formatted(metric, target, additionalJson)
        );
        challenge.setEnabled(true);
        challenge.setSchemaVersion(3);
        return challenge;
    }

    /**
     * Associates one challenge with the competition week.
     */
    private WeeklyChallenge createWeeklyChallenge(Challenge challenge) {
        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(COMPETITION_WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(CALCULATION_TIME.minusSeconds(3_600));
        return weeklyChallenge;
    }

    /**
     * Loads weekly challenges in persistence order.
     */
    private List<WeeklyChallenge> loadChallenges(LocalDate weekStart) {
        return weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(weekStart);
    }

    /**
     * Loads weekly scores in ranking order.
     */
    private List<WeeklyPlayerScore> loadScores(LocalDate weekStart) {
        return scoreRepository.findAllByWeekStartOrderByPositionAsc(weekStart);
    }

    /**
     * Supplies a mutable UTC clock to cross a week boundary in one test.
     */
    @TestConfiguration
    static class MutableClockConfiguration {

        /**
         * Creates the mutable primary application clock.
         */
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(CALCULATION_TIME, ZoneOffset.UTC);
        }
    }

    /**
     * Thread-safe clock whose instant can be advanced by an integration test.
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
         * Returns a clock sharing the same mutable instant in another zone.
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
