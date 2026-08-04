package io.github.thomashtn.valorant.tracker.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.thomashtn.valorant.tracker.challenge.calculator.AllChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeMatchFilter;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeMetricEvaluator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valorant.tracker.challenge.calculator.CountMatchesChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.DistinctCountChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.MaxGroupChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.MaxStreakChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valorant.tracker.challenge.calculator.RatioChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.SumChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCategory;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valorant.tracker.challenge.parser.JacksonChallengeDefinitionParser;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies that the production challenge catalogue remains compatible with
 * the parser and every registered progress calculator.
 */
class ChallengeCatalogueCompatibilityTest {

    /**
     * Production migration containing the challenge catalogue.
     */
    private static final String CATALOGUE_RESOURCE =
        "db/migration/V3__insert_challenges.sql";

    /**
     * Expected number of active catalogue entries.
     *
     * <p>V3 seeds 78 rows, of which V14 deletes the 16 filtered on a game mode synchronization no
     * longer imports.
     */
    private static final int EXPECTED_CHALLENGE_COUNT = 62;

    /**
     * Game-mode filters removed from the catalogue by V14.
     *
     * <p>The migration deletes the rows still present in V3, so this test must skip them the same
     * way: mirroring the migration's predicate rather than listing challenge codes keeps the two in
     * step if another challenge with the same filter is ever added.
     */
    private static final Pattern REMOVED_GAME_MODE_PATTERN = Pattern.compile(
        "\"gameMode\"\\s*:\\s*\"(SWIFTPLAY|ESCALATION)\""
    );

    /**
     * Pattern extracting one challenge row from the production migration.
     */
    private static final Pattern CHALLENGE_ROW_PATTERN = Pattern.compile(
        "\\('([^']*)','([^']*)','([^']*)','([^']*)',(\\d+),"
            + "'([^']*)','([^']*)','([^']*)','(\\[.*?])'::jsonb,"
            + "(NULL|'[^']*'),(TRUE|FALSE),(\\d+)\\)",
        Pattern.DOTALL
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
            new ChallengeMetricEvaluator();
        ChallengeMatchFilter matchFilter = new ChallengeMatchFilter();

        List<ChallengeProgressCalculator> calculators = List.of(
            new SumChallengeProgressCalculator(
                metricEvaluator,
                matchFilter
            ),
            new CountMatchesChallengeProgressCalculator(
                metricEvaluator,
                matchFilter
            ),
            new DistinctCountChallengeProgressCalculator(
                metricEvaluator,
                matchFilter,
                new WeekCalendar(Clock.systemUTC(), ZoneOffset.UTC)
            ),
            new MaxGroupChallengeProgressCalculator(
                metricEvaluator,
                matchFilter,
                new WeekCalendar(Clock.systemUTC(), ZoneOffset.UTC)
            ),
            new AllChallengeProgressCalculator(
                metricEvaluator,
                matchFilter
            ),
            new RatioChallengeProgressCalculator(matchFilter),
            new MaxStreakChallengeProgressCalculator(
                metricEvaluator,
                matchFilter
            )
        );

        definitionParser = new JacksonChallengeDefinitionParser(
            JsonMapper.builder().build()
        );
        calculatorRegistry =
            new ChallengeProgressCalculatorRegistry(calculators);
        emptyContext = new PlayerChallengeContext(
            1L,
            LocalDate.of(2026, 7, 20),
            Instant.parse("2026-07-20T00:00:00Z"),
            Instant.parse("2026-07-27T00:00:00Z"),
            List.of()
        );
    }

    /**
     * Verifies that every production rule can be parsed and calculated.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldParseAndCalculateEveryProductionChallenge()
        throws IOException {
        List<Challenge> challenges = loadChallenges();

        assertThat(challenges)
            .as("production challenge count")
            .hasSize(EXPECTED_CHALLENGE_COUNT);

        for (Challenge challenge : challenges) {
            assertThatCode(() -> calculate(challenge))
                .as("compatibility of challenge %s", challenge.getCode())
                .doesNotThrowAnyException();

            ChallengeProgressResult result = calculate(challenge);

            assertThat(result).isNotNull();
            assertThat(result.currentValue())
                .isNotNull()
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(result.targetValue())
                .isNotNull()
                .isGreaterThan(BigDecimal.ZERO);
            assertThat(result.progressPercentage())
                .isNotNull()
                .isBetween(
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(100)
                );
            assertThat(result.completed()).isFalse();
        }
    }

    /**
     * Verifies unique codes and complete progress-mode coverage.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldContainUniqueCodesAndEveryProgressMode()
        throws IOException {
        List<Challenge> challenges = loadChallenges();
        Set<String> uniqueCodes = new HashSet<>();
        Set<ProgressMode> catalogueModes = EnumSet.noneOf(
            ProgressMode.class
        );

        for (Challenge challenge : challenges) {
            assertThat(uniqueCodes.add(challenge.getCode()))
                .as("unique challenge code %s", challenge.getCode())
                .isTrue();
            catalogueModes.add(challenge.getProgressMode());
        }

        assertThat(catalogueModes)
            .containsExactlyInAnyOrderElementsOf(
                EnumSet.allOf(ProgressMode.class)
            );
    }

    /**
     * Verifies registry coverage for every supported progress mode.
     */
    @Test
    void shouldRegisterCalculatorForEveryProgressMode() {
        for (ProgressMode progressMode : ProgressMode.values()) {
            assertThat(calculatorRegistry.supports(progressMode)).isTrue();
            assertThat(
                calculatorRegistry
                    .getCalculator(progressMode)
                    .supportedMode()
            ).isEqualTo(progressMode);
        }
    }

    /**
     * Parses and calculates one challenge through production components.
     *
     * @param challenge persisted challenge definition
     * @return normalized calculation result
     */
    private ChallengeProgressResult calculate(Challenge challenge) {
        ChallengeDefinition definition = definitionParser.parse(challenge);
        ChallengeProgressCalculator calculator =
            calculatorRegistry.getCalculator(
                definition.progressMode()
            );

        return calculator.calculate(definition, emptyContext);
    }

    /**
     * Reads and converts every challenge row from the migration.
     *
     * @return challenges in declaration order
     * @throws IOException when the migration cannot be read
     */
    private List<Challenge> loadChallenges() throws IOException {
        Matcher matcher = CHALLENGE_ROW_PATTERN.matcher(
            readCatalogueMigration()
        );
        List<Challenge> challenges = new ArrayList<>();

        while (matcher.find()) {
            if (REMOVED_GAME_MODE_PATTERN.matcher(matcher.group(9)).find()) {
                continue;
            }
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

        challenge.setCode(matcher.group(1));
        challenge.setName(matcher.group(2));
        challenge.setDescription(matcher.group(3));
        challenge.setDifficulty(
            ChallengeDifficulty.valueOf(matcher.group(4))
        );
        challenge.setDamage(Integer.parseInt(matcher.group(5)));
        challenge.setCategory(
            ChallengeCategory.valueOf(matcher.group(6))
        );
        challenge.setRuleType(
            ChallengeRuleType.valueOf(matcher.group(7))
        );
        challenge.setProgressMode(
            ProgressMode.valueOf(matcher.group(8))
        );
        challenge.setConditionsJson(matcher.group(9));
        challenge.setExclusionGroup(
            parseNullableSqlString(matcher.group(10))
        );
        challenge.setEnabled(
            Boolean.parseBoolean(matcher.group(11))
        );
        challenge.setSchemaVersion(
            Integer.parseInt(matcher.group(12))
        );

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
        ClassLoader classLoader = Thread.currentThread()
            .getContextClassLoader();

        try (InputStream inputStream =
                 classLoader.getResourceAsStream(CATALOGUE_RESOURCE)) {
            assertThat(inputStream)
                .as("production challenge migration")
                .isNotNull();

            return new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
            );
        }
    }
}
