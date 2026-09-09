package io.github.thomashtn.valoquests.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.thomashtn.valoquests.challenge.calculator.AggregateRateCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.AllChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.BaselineChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeMatchFilter;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeMetricEvaluator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.CountMatchesChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.DistinctCountChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.MaxGroupChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.MaxStreakChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.RatioChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.SumChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.parser.JacksonChallengeDefinitionParser;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies that the production challenge catalogue remains compatible with the parser and every
 * registered progress calculator, at both squad levels, and that it obeys the rules of
 * {@code docs/CHALLENGES.md}.
 *
 * <p>The rules are checked on the content of each row, never on a list of codes, so a challenge
 * added later falls under the same rules instead of slipping past them.
 */
class ChallengeCatalogueCompatibilityTest {

    /**
     * Production migration seeding the whole catalogue.
     */
    private static final String CATALOGUE_MIGRATION =
        "db/migration/V47__hand_written_challenge_catalogue.sql";

    /**
     * One SQL string literal, doubled quotes included: French copy contains apostrophes.
     */
    private static final String QUOTED = "'((?:[^']|'')*)'";

    /**
     * Pattern extracting one challenge row: nullable difficulty, two rule grids, cadence last.
     */
    private static final Pattern ROW = Pattern.compile(
        "\\(" + QUOTED + "," + QUOTED + "," + QUOTED + ",(NULL|" + QUOTED + "),"
            + QUOTED + "," + QUOTED + ",'(\\[.*?])'::jsonb,'(\\[.*?])'::jsonb,"
            + "(NULL|" + QUOTED + "),(TRUE|FALSE),(\\d+)," + QUOTED + "\\)",
        Pattern.DOTALL
    );

    /**
     * Weekly entries expected per difficulty: two campaigns without a repeat inside one tier.
     */
    private static final int WEEKLY_PER_DIFFICULTY = 20;

    /**
     * Daily entries expected: four weeks without a repeat.
     */
    private static final int DAILY_POOL_SIZE = 28;

    /**
     * Cap on the agents a challenge may ask for, in either direction.
     */
    private static final int AGENT_CAP = 3;

    /**
     * Matches a per-match bar may ask for on a daily challenge.
     */
    private static final int DAILY_OCCURRENCE_CAP = 2;

    /**
     * Statistics the Henrik API never reports outside round-based modes.
     *
     * <p>Headshots and damage come back as zero on every deathmatch and every skirmish row. A
     * challenge measuring them there can never be completed, whatever its target says.
     */
    private static final Set<ChallengeMetric> ROUND_BASED_ONLY_METRICS =
        EnumSet.of(ChallengeMetric.HEADSHOTS, ChallengeMetric.DAMAGE_DEALT);

    /**
     * Modes the API reports no headshot and no damage for.
     */
    private static final Set<ChallengeGameMode> BLIND_MODES =
        EnumSet.of(ChallengeGameMode.DEATHMATCH, ChallengeGameMode.SKIRMISH);

    /**
     * Progress modes the catalogue still declares.
     *
     * <p>Ratios held across the week, streaks and baselines are deliberately absent: each could be
     * lost by one bad match, or decided before the week opened. The modes and their calculators
     * stay registered — {@link #shouldRegisterCalculatorForEveryProgressMode()} still covers them.
     */
    private static final Set<ProgressMode> EXPECTED_CATALOGUE_MODES = EnumSet.of(
        ProgressMode.SUM,
        ProgressMode.COUNT_MATCHES,
        ProgressMode.DISTINCT_COUNT,
        ProgressMode.MAX_GROUP
    );

    /**
     * Parser used to validate catalogue definitions.
     */
    private ChallengeDefinitionParser definitionParser;

    /**
     * Registry containing every production progress calculator.
     */
    private ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Empty weekly context used for compatibility calculations.
     */
    private PlayerChallengeContext emptyContext;

    /**
     * Creates the production parser, registry and calculation context.
     */
    @BeforeEach
    void setUp() {
        ChallengeMetricEvaluator metricEvaluator =
            new ChallengeMetricEvaluator(new MatchOutcomeResolver());
        ChallengeMatchFilter matchFilter = new ChallengeMatchFilter(new MatchEligibility());
        WeekCalendar weekCalendar = new WeekCalendar(Clock.systemUTC(), ZoneOffset.UTC);

        List<ChallengeProgressCalculator> calculators = List.of(
            new SumChallengeProgressCalculator(metricEvaluator, matchFilter),
            new CountMatchesChallengeProgressCalculator(metricEvaluator, matchFilter),
            new DistinctCountChallengeProgressCalculator(metricEvaluator, matchFilter, weekCalendar),
            new MaxGroupChallengeProgressCalculator(metricEvaluator, matchFilter, weekCalendar),
            new AllChallengeProgressCalculator(metricEvaluator, matchFilter),
            new RatioChallengeProgressCalculator(matchFilter, new AggregateRateCalculator()),
            new MaxStreakChallengeProgressCalculator(metricEvaluator, matchFilter),
            new BaselineChallengeProgressCalculator(new AggregateRateCalculator(), matchFilter)
        );

        definitionParser = new JacksonChallengeDefinitionParser(JsonMapper.builder().build());
        calculatorRegistry = new ChallengeProgressCalculatorRegistry(calculators);
        emptyContext = new PlayerChallengeContext(
            1L,
            LocalDate.of(2026, 7, 20),
            Instant.parse("2026-07-20T00:00:00Z"),
            Instant.parse("2026-07-27T00:00:00Z"),
            List.of()
        );
    }

    /**
     * Verifies that both grids of every production rule parse and calculate.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldParseAndCalculateEveryProductionChallengeAtBothLevels() throws IOException {
        List<Challenge> challenges = loadChallenges();

        assertThat(challenges)
            .as("production challenge count")
            .hasSize(WEEKLY_PER_DIFFICULTY * ChallengeDifficulty.values().length + DAILY_POOL_SIZE);

        for (Challenge challenge : challenges) {
            for (CampaignDifficulty level : CampaignDifficulty.values()) {
                assertThatCode(() -> calculate(challenge, level))
                    .as("compatibility of %s at %s", challenge.getCode(), level)
                    .doesNotThrowAnyException();

                ChallengeProgressResult result = calculate(challenge, level);

                assertThat(result.currentValue()).isNotNull().isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(result.targetValue())
                    .as("target of %s at %s", challenge.getCode(), level)
                    .isNotNull()
                    .isGreaterThan(BigDecimal.ZERO);
                assertThat(result.progressPercentage())
                    .isNotNull()
                    .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
                assertThat(result.completed()).isFalse();
            }
        }
    }

    /**
     * Verifies unique codes, the tier sizes and the progress modes the catalogue declares.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldHoldTwentyPerTierAndTwentyEightDailies() throws IOException {
        List<Challenge> challenges = loadChallenges();
        Set<String> uniqueCodes = new HashSet<>();
        EnumMap<ChallengeDifficulty, Integer> weeklyByDifficulty =
            new EnumMap<>(ChallengeDifficulty.class);
        int dailies = 0;

        for (Challenge challenge : challenges) {
            assertThat(uniqueCodes.add(challenge.getCode()))
                .as("unique challenge code %s", challenge.getCode())
                .isTrue();
            assertThat(challenge.isEnabled()).as("%s enabled", challenge.getCode()).isTrue();

            if (challenge.getCadence() == ChallengeCadence.DAILY) {
                assertThat(challenge.getDifficulty()).as("%s has no tier", challenge.getCode()).isNull();
                dailies++;
            } else {
                assertThat(challenge.getDifficulty()).as("%s has a tier", challenge.getCode()).isNotNull();
                weeklyByDifficulty.merge(challenge.getDifficulty(), 1, Integer::sum);
            }
        }

        assertThat(dailies).isEqualTo(DAILY_POOL_SIZE);
        assertThat(weeklyByDifficulty)
            .allSatisfy((difficulty, count) ->
                assertThat(count).as(difficulty.name()).isEqualTo(WEEKLY_PER_DIFFICULTY));
        assertThat(weeklyByDifficulty.keySet()).containsExactlyInAnyOrder(ChallengeDifficulty.values());
        assertThat(challenges.stream().map(Challenge::getProgressMode).collect(Collectors.toSet()))
            .containsExactlyInAnyOrderElementsOf(EXPECTED_CATALOGUE_MODES);
    }

    /**
     * Verifies that the expert grid asks the same question as the reference one, never a lower one.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldNeverWriteAnExpertGridBelowItsReference() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            List<ChallengeCondition> reference =
                definitionParser.parse(challenge, CampaignDifficulty.AMATEUR).conditions();
            List<ChallengeCondition> expert =
                definitionParser.parse(challenge, CampaignDifficulty.PRO).conditions();

            assertThat(expert)
                .as("%s declares the same conditions at both levels", challenge.getCode())
                .hasSameSizeAs(reference);

            for (int index = 0; index < reference.size(); index++) {
                ChallengeCondition base = reference.get(index);
                ChallengeCondition harder = expert.get(index);

                assertThat(harder.metric()).as(challenge.getCode()).isEqualTo(base.metric());
                assertThat(harder.effectiveGameMode())
                    .as(challenge.getCode())
                    .isEqualTo(base.effectiveGameMode());
                assertThat(harder.groupBy()).as(challenge.getCode()).isEqualTo(base.groupBy());
                assertThat(harder.target())
                    .as("expert target of %s", challenge.getCode())
                    .isGreaterThanOrEqualTo(base.target());

                if (base.occurrences() != null) {
                    assertThat(harder.occurrences())
                        .as("expert occurrences of %s", challenge.getCode())
                        .isGreaterThanOrEqualTo(base.occurrences());
                }
            }
        }
    }

    /**
     * Verifies rule one: competitive is required by the hardest tier only, never by a daily.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldRequireCompetitiveInTheHardestTierOnly() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            boolean competitiveOnly = definitionParser.parse(challenge).isCompetitiveOnly();
            boolean hardestWeekly = challenge.getCadence() == ChallengeCadence.WEEKLY
                && challenge.getDifficulty() == ChallengeDifficulty.VERY_HARD;

            if (!hardestWeekly) {
                assertThat(competitiveOnly)
                    .as("%s must not require competitive", challenge.getCode())
                    .isFalse();
            }
        }
    }

    /**
     * Verifies rule three: no challenge asks for more than three agents.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldCapAgentsAtThree() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            for (ChallengeCondition condition : everyCondition(challenge)) {
                if (condition.groupBy() == ChallengeGroupBy.AGENT) {
                    assertThat(condition.target())
                        .as("%s agent cap", challenge.getCode())
                        .isLessThanOrEqualTo(BigDecimal.valueOf(AGENT_CAP));
                }
            }
        }
    }

    /**
     * Verifies that a daily bar is never spread over more than two matches.
     *
     * <p>A daily may ask for a volume of matches — the catalogue writes up to six — but a bar to
     * clear match after match stays inside two, otherwise a single bad game costs the day.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldKeepDailyBarsWithinTwoMatches() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            if (challenge.getCadence() != ChallengeCadence.DAILY) {
                continue;
            }

            for (ChallengeCondition condition : everyCondition(challenge)) {
                if (condition.occurrences() != null) {
                    assertThat(condition.occurrences())
                        .as(challenge.getCode())
                        .isLessThanOrEqualTo(DAILY_OCCURRENCE_CAP);
                }
            }
        }
    }

    /**
     * Verifies that no challenge measures a statistic its mode never reports.
     *
     * <p>The Henrik payload returns zero headshots and zero damage on every deathmatch and every
     * skirmish. A challenge asking for either there is not hard, it is impossible.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldNeverMeasureHeadshotsOrDamageInABlindMode() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            for (ChallengeCondition condition : everyCondition(challenge)) {
                boolean blind = ROUND_BASED_ONLY_METRICS.contains(condition.metric())
                    && BLIND_MODES.contains(condition.effectiveGameMode());

                assertThat(blind)
                    .as("%s measures %s in %s, which the API never reports",
                        challenge.getCode(), condition.metric(), condition.effectiveGameMode())
                    .isFalse();
            }
        }
    }

    /**
     * Verifies that no challenge requires deathmatch and team deathmatch at once.
     *
     * <p>Both modes are played in bursts and rarely in the same week, so a challenge needing a
     * volume of each is decided by the squad's habits rather than by its play.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldNeverCombineDeathmatchAndTeamDeathmatch() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            Set<ChallengeGameMode> modes = everyCondition(challenge).stream()
                .map(ChallengeCondition::effectiveGameMode)
                .collect(Collectors.toSet());

            assertThat(modes.containsAll(
                Set.of(ChallengeGameMode.DEATHMATCH, ChallengeGameMode.TEAM_DEATHMATCH)
            )).as("%s combines both short formats", challenge.getCode()).isFalse();
        }
    }

    /**
     * Verifies that every declared game-mode filter is one the model knows.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldOnlyDeclareKnownGameModeFilters() throws IOException {
        Set<String> knownFilters = Arrays.stream(ChallengeGameMode.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
        Pattern gameModePattern = Pattern.compile("\"gameMode\"\\s*:\\s*\"([A-Z_]+)\"");

        for (Challenge challenge : loadChallenges()) {
            for (String json : List.of(challenge.getConditionsJson(), challenge.getExpertConditionsJson())) {
                Matcher matcher = gameModePattern.matcher(json);

                while (matcher.find()) {
                    assertThat(knownFilters).as(challenge.getCode()).contains(matcher.group(1));
                }
            }
        }
    }

    /**
     * Verifies registry coverage for every supported progress mode.
     */
    @Test
    void shouldRegisterCalculatorForEveryProgressMode() {
        for (ProgressMode progressMode : ProgressMode.values()) {
            assertThat(calculatorRegistry.supports(progressMode)).isTrue();
            assertThat(calculatorRegistry.getCalculator(progressMode).supportedMode())
                .isEqualTo(progressMode);
        }
    }

    /**
     * Returns every condition a challenge declares, at both levels.
     *
     * @param challenge challenge to read
     * @return the conditions of both grids
     */
    private List<ChallengeCondition> everyCondition(Challenge challenge) {
        List<ChallengeCondition> conditions =
            new ArrayList<>(definitionParser.parse(challenge, CampaignDifficulty.AMATEUR).conditions());

        conditions.addAll(definitionParser.parse(challenge, CampaignDifficulty.PRO).conditions());

        return conditions;
    }

    /**
     * Parses and calculates one challenge grid through production components.
     *
     * @param challenge persisted challenge definition
     * @param level     squad level whose grid is read
     * @return normalized calculation result
     */
    private ChallengeProgressResult calculate(Challenge challenge, CampaignDifficulty level) {
        ChallengeDefinition definition = definitionParser.parse(challenge, level);

        return calculatorRegistry.getCalculator(definition.progressMode())
            .calculate(definition, emptyContext);
    }

    /**
     * Reads and converts every challenge row from the migration.
     *
     * @return challenges in declaration order
     * @throws IOException when the migration cannot be read
     */
    private List<Challenge> loadChallenges() throws IOException {
        List<Challenge> challenges = new ArrayList<>();
        Matcher matcher = ROW.matcher(readCatalogueMigration());

        while (matcher.find()) {
            challenges.add(toChallenge(matcher));
        }

        return List.copyOf(challenges);
    }

    /**
     * Converts one matched SQL row into a challenge entity.
     *
     * @param matcher matcher positioned on one challenge row
     * @return reconstructed challenge entity
     */
    private Challenge toChallenge(Matcher matcher) {
        Challenge challenge = new Challenge();
        String difficulty = parseNullableSqlString(matcher.group(4));

        challenge.setCode(matcher.group(1));
        challenge.setName(unescape(matcher.group(2)));
        challenge.setDescription(unescape(matcher.group(3)));
        challenge.setDifficulty(difficulty == null ? null : ChallengeDifficulty.valueOf(difficulty));
        challenge.setCategory(ChallengeCategory.valueOf(matcher.group(6)));
        challenge.setProgressMode(ProgressMode.valueOf(matcher.group(7)));
        challenge.setConditionsJson(matcher.group(8));
        challenge.setExpertConditionsJson(matcher.group(9));
        challenge.setExclusionGroup(parseNullableSqlString(matcher.group(10)));
        challenge.setEnabled(Boolean.parseBoolean(matcher.group(12)));
        challenge.setSchemaVersion(Integer.parseInt(matcher.group(13)));
        challenge.setCadence(ChallengeCadence.valueOf(matcher.group(14)));

        return challenge;
    }

    /**
     * Restores the single quotes an SQL literal doubles.
     *
     * @param sqlValue value read out of a literal
     * @return the value as written in the catalogue
     */
    private String unescape(String sqlValue) {
        return sqlValue.replace("''", "'");
    }

    /**
     * Converts a nullable SQL literal into its Java representation.
     *
     * @param sqlValue SQL value
     * @return unquoted value or {@code null}
     */
    private String parseNullableSqlString(String sqlValue) {
        if ("NULL".equals(sqlValue)) {
            return null;
        }

        return unescape(sqlValue.substring(1, sqlValue.length() - 1));
    }

    /**
     * Reads the production challenge migration from the classpath.
     *
     * @return complete migration content
     * @throws IOException when the resource is missing or unreadable
     */
    private String readCatalogueMigration() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(CATALOGUE_MIGRATION)) {
            assertThat(inputStream).as("production challenge migration").isNotNull();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
