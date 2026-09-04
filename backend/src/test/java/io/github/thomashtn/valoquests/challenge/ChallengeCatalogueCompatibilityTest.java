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
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.parser.JacksonChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.service.ChallengeTargetResolver;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies that the production challenge catalogue remains compatible with the parser, the target
 * resolver and every registered progress calculator, and that it obeys the rules of
 * {@code docs/CHALLENGES.md}.
 *
 * <p>The rules are checked on the content of each row, never on a list of codes, so a challenge
 * added later falls under the same rules instead of slipping past them.
 */
class ChallengeCatalogueCompatibilityTest {

    /**
     * Production migration seeding the whole catalogue.
     */
    private static final String CATALOGUE_MIGRATION = "db/migration/V41__gameplay_v2_challenge_catalogue.sql";

    /**
     * Pattern extracting one challenge row: nullable difficulty, then the cadence last.
     */
    private static final Pattern ROW = Pattern.compile(
        "\\('([^']*)','([^']*)','([^']*)',(NULL|'[^']*'),"
            + "'([^']*)','([^']*)','(\\[.*?])'::jsonb,"
            + "(NULL|'[^']*'),(TRUE|FALSE),(\\d+),'([^']*)'\\)",
        Pattern.DOTALL
    );

    /**
     * Weekly entries expected per difficulty: two campaigns without a repeat inside one tier.
     */
    private static final int WEEKLY_PER_DIFFICULTY = 20;

    /**
     * Daily entries expected: three weeks without a repeat.
     */
    private static final int DAILY_POOL_SIZE = 21;

    /**
     * Cap on the agents a challenge may ask for, in either direction.
     */
    private static final int AGENT_CAP = 3;

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
        ProgressMode.MAX_GROUP,
        ProgressMode.ALL
    );

    /**
     * Parser used to validate catalogue definitions.
     */
    private ChallengeDefinitionParser definitionParser;

    /**
     * Resolver used to scale catalogue definitions.
     */
    private ChallengeTargetResolver targetResolver;

    /**
     * Registry containing every production progress calculator.
     */
    private ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Empty weekly context used for compatibility calculations.
     */
    private PlayerChallengeContext emptyContext;

    /**
     * Creates the production parser, resolver, registry and calculation context.
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
        targetResolver = new ChallengeTargetResolver();
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
     * Verifies that every production rule can be parsed, resolved at both ends of the volume
     * range, and calculated.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldParseResolveAndCalculateEveryProductionChallenge() throws IOException {
        List<Challenge> challenges = loadChallenges();

        assertThat(challenges)
            .as("production challenge count")
            .hasSize(WEEKLY_PER_DIFFICULTY * ChallengeDifficulty.values().length + DAILY_POOL_SIZE);

        for (Challenge challenge : challenges) {
            for (ChallengeScaling scaling : List.of(ChallengeScaling.NONE, amateurScaling(), eliteScaling())) {
                assertThatCode(() -> calculate(challenge, scaling))
                    .as("compatibility of challenge %s", challenge.getCode())
                    .doesNotThrowAnyException();

                ChallengeProgressResult result = calculate(challenge, scaling);

                assertThat(result.currentValue()).isNotNull().isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(result.targetValue())
                    .as("resolved target of %s", challenge.getCode())
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
    void shouldHoldTwentyPerTierAndTwentyOneDailies() throws IOException {
        List<Challenge> challenges = loadChallenges();
        Set<String> uniqueCodes = new HashSet<>();
        Map<ChallengeDifficulty, Integer> weeklyByDifficulty = new EnumMap<>(ChallengeDifficulty.class);
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
            for (ChallengeCondition condition : definitionParser.parse(challenge).conditions()) {
                if (condition.groupBy() == ChallengeGroupBy.AGENT) {
                    assertThat(condition.target())
                        .as("%s agent cap", challenge.getCode())
                        .isLessThanOrEqualTo(BigDecimal.valueOf(AGENT_CAP));
                }
            }
        }
    }

    /**
     * Verifies rule five: a daily is decided in at most two matches, whatever the mode, and never
     * declares an unresolved filter that the parser could not read.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldKeepDailiesWithinTwoMatches() throws IOException {
        for (Challenge challenge : loadChallenges()) {
            if (challenge.getCadence() != ChallengeCadence.DAILY) {
                continue;
            }

            ChallengeDefinition definition = definitionParser.parse(challenge);

            for (ChallengeCondition condition : definition.conditions()) {
                if (condition.occurrences() != null) {
                    assertThat(condition.occurrences()).as(challenge.getCode()).isLessThanOrEqualTo(2);
                }

                if (condition.isMatchCountMetric() && condition.groupBy() == null) {
                    assertThat(condition.target()).as(challenge.getCode())
                        .isLessThanOrEqualTo(BigDecimal.valueOf(2));
                }
            }
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
            Matcher matcher = gameModePattern.matcher(challenge.getConditionsJson());

            while (matcher.find()) {
                assertThat(knownFilters).as(challenge.getCode()).contains(matcher.group(1));
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
     * Parses, resolves and calculates one challenge through production components.
     *
     * @param challenge persisted challenge definition
     * @param scaling   scaling to resolve against
     * @return normalized calculation result
     */
    private ChallengeProgressResult calculate(Challenge challenge, ChallengeScaling scaling) {
        ChallengeDefinition definition = targetResolver.resolve(
            definitionParser.parse(challenge),
            challenge.getCadence(),
            challenge.getDifficulty(),
            scaling
        );

        return calculatorRegistry.getCalculator(definition.progressMode())
            .calculate(definition, emptyContext);
    }

    /**
     * Scaling of the weakest squad the catalogue is meant for.
     *
     * @return amateur scaling
     */
    private ChallengeScaling amateurScaling() {
        return new ChallengeScaling(
            new BigDecimal("0.4"),
            Map.of(
                SkillAnchor.LONG_KILLS, BigDecimal.valueOf(10),
                SkillAnchor.LONG_HEADSHOTS, BigDecimal.valueOf(5),
                SkillAnchor.LONG_ASSISTS, BigDecimal.valueOf(4),
                SkillAnchor.LONG_SCORE, BigDecimal.valueOf(3_000),
                SkillAnchor.LONG_KD, new BigDecimal("0.8"),
                SkillAnchor.LONG_ADR, BigDecimal.valueOf(100),
                SkillAnchor.LONG_ACS, BigDecimal.valueOf(160),
                SkillAnchor.DEATHMATCH_KILLS, BigDecimal.valueOf(18),
                SkillAnchor.DEATHMATCH_HEADSHOTS, BigDecimal.valueOf(7),
                SkillAnchor.TEAM_DEATHMATCH_KILLS, BigDecimal.valueOf(20)
            )
        );
    }

    /**
     * Scaling of a squad at the volume bound, without measured anchors.
     *
     * @return elite scaling
     */
    private ChallengeScaling eliteScaling() {
        return new ChallengeScaling(BigDecimal.valueOf(3), Map.of());
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
        challenge.setName(matcher.group(2));
        challenge.setDescription(matcher.group(3));
        challenge.setDifficulty(difficulty == null ? null : ChallengeDifficulty.valueOf(difficulty));
        challenge.setCategory(ChallengeCategory.valueOf(matcher.group(5)));
        challenge.setProgressMode(ProgressMode.valueOf(matcher.group(6)));
        challenge.setConditionsJson(matcher.group(7));
        challenge.setExclusionGroup(parseNullableSqlString(matcher.group(8)));
        challenge.setEnabled(Boolean.parseBoolean(matcher.group(9)));
        challenge.setSchemaVersion(Integer.parseInt(matcher.group(10)));
        challenge.setCadence(ChallengeCadence.valueOf(matcher.group(11)));

        return challenge;
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

        return sqlValue.substring(1, sqlValue.length() - 1);
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
