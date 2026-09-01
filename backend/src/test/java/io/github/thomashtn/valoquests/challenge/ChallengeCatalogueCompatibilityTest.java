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
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
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
     * Pattern extracting one challenge row seeded before the per-challenge damage column was dropped.
     */
    private static final Pattern ROW_WITH_DAMAGE_AND_RULE_TYPE = Pattern.compile(
        "\\('([^']*)','([^']*)','([^']*)','([^']*)',(\\d+),"
            + "'([^']*)','([^']*)','([^']*)','(\\[.*?])'::jsonb,"
            + "(NULL|'[^']*'),(TRUE|FALSE),(\\d+)\\)",
        Pattern.DOTALL
    );

    /**
     * Pattern extracting one challenge row written between the damage column being dropped and
     * {@code rule_type} being dropped by V30.
     */
    private static final Pattern ROW_WITH_RULE_TYPE = Pattern.compile(
        "\\('([^']*)','([^']*)','([^']*)','([^']*)',"
            + "'([^']*)','([^']*)','([^']*)','(\\[.*?])'::jsonb,"
            + "(NULL|'[^']*'),(TRUE|FALSE),(\\d+)\\)",
        Pattern.DOTALL
    );

    /**
     * Pattern extracting one challenge row in the shape the table has today, after V30 dropped
     * {@code rule_type} for duplicating {@code progress_mode}.
     */
    private static final Pattern CURRENT_ROW = Pattern.compile(
        "\\('([^']*)','([^']*)','([^']*)','([^']*)',"
            + "'([^']*)','([^']*)','(\\[.*?])'::jsonb,"
            + "(NULL|'[^']*'),(TRUE|FALSE),(\\d+)\\)",
        Pattern.DOTALL
    );

    /**
     * Production migrations seeding the challenge catalogue, in the order they are applied.
     *
     * <p>Each carries the row shape the table had when it was written, since the columns between
     * the difficulty and the conditions moved twice.
     */
    private static final List<CatalogueMigration> CATALOGUE_MIGRATIONS = List.of(
        new CatalogueMigration(
            "db/migration/V3__insert_challenges.sql",
            ROW_WITH_DAMAGE_AND_RULE_TYPE,
            6
        ),
        new CatalogueMigration(
            "db/migration/V28__add_progression_challenges.sql",
            ROW_WITH_RULE_TYPE,
            5
        ),
        new CatalogueMigration(
            "db/migration/V39__add_weekly_skill_challenges.sql",
            CURRENT_ROW,
            5
        )
    );

    /**
     * Expected number of catalogue entries.
     *
     * <p>V3 seeds 78 rows, of which V14 deletes the 16 filtered on a game mode synchronization no
     * longer imports, leaving 62. V28 adds 7 progression challenges and V38 deletes them again,
     * back to 62. V39 adds 14 weekly skill challenges. The 6 volume challenges V28 disables are
     * still counted here: it disables them with an UPDATE rather than removing the rows, and their
     * definitions must keep parsing and calculating for the finalized weeks that drew them.
     */
    private static final int EXPECTED_CHALLENGE_COUNT = 76;

    /**
     * Progress modes the catalogue still declares.
     *
     * <p>{@link ProgressMode#BASELINE} is deliberately absent: V38 deleted every challenge using it,
     * because measuring a week against the four before it decided half the outcome before the week
     * opened. The mode and its calculator stay registered — {@link
     * #shouldRegisterCalculatorForEveryProgressMode()} still covers them — so a single-week
     * progression challenge can be written later without reinstating anything.
     */
    private static final Set<ProgressMode> EXPECTED_CATALOGUE_MODES = EnumSet.complementOf(
        EnumSet.of(ProgressMode.BASELINE)
    );

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
            new RatioChallengeProgressCalculator(matchFilter, new AggregateRateCalculator()),
            new MaxStreakChallengeProgressCalculator(
                metricEvaluator,
                matchFilter
            ),
            new BaselineChallengeProgressCalculator(
                new AggregateRateCalculator(),
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
     * Verifies unique codes and the progress modes the catalogue is expected to declare.
     *
     * @throws IOException when the production migration cannot be read
     */
    @Test
    void shouldContainUniqueCodesAndTheExpectedProgressModes()
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
            .containsExactlyInAnyOrderElementsOf(EXPECTED_CATALOGUE_MODES);
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
        List<Challenge> challenges = new ArrayList<>();

        for (CatalogueMigration migration : CATALOGUE_MIGRATIONS) {
            Matcher matcher = migration.rowPattern()
                .matcher(readCatalogueMigration(migration.resource()));

            while (matcher.find()) {
                Challenge challenge = toChallenge(matcher, migration.categoryGroup());

                if (isDeletedByLaterMigration(challenge)) {
                    continue;
                }

                challenges.add(challenge);
            }
        }

        return List.copyOf(challenges);
    }

    /**
     * Determines whether a seeded row was removed again by a later migration.
     *
     * <p>Mirrors the predicates V14 and V38 delete on — a retired game-mode filter, and the baseline
     * progress mode — rather than listing challenge codes, so a challenge added later with either
     * shape is caught by the same rule instead of surviving unnoticed.
     *
     * @param challenge challenge reconstructed from a seeding migration
     * @return {@code true} when the row is not in the catalogue any more
     */
    private boolean isDeletedByLaterMigration(Challenge challenge) {
        return REMOVED_GAME_MODE_PATTERN.matcher(challenge.getConditionsJson()).find()
            || challenge.getProgressMode() == ProgressMode.BASELINE;
    }

    /**
     * Converts one matched SQL row into a challenge entity.
     *
     * <p>The columns between the difficulty and the category moved as the table lost its damage and
     * {@code rule_type} columns, so the category is the one group each migration has to declare. The
     * five trailing columns are counted back from the end, where every shape agrees.
     *
     * @param matcher       matcher positioned on one challenge row
     * @param categoryGroup capture group holding the category in this row shape
     * @return reconstructed challenge entity
     */
    private Challenge toChallenge(Matcher matcher, int categoryGroup) {
        int lastGroup = matcher.groupCount();
        Challenge challenge = new Challenge();

        challenge.setCode(matcher.group(1));
        challenge.setName(matcher.group(2));
        challenge.setDescription(matcher.group(3));
        challenge.setDifficulty(
            ChallengeDifficulty.valueOf(matcher.group(4))
        );
        challenge.setCategory(
            ChallengeCategory.valueOf(matcher.group(categoryGroup))
        );
        challenge.setProgressMode(
            ProgressMode.valueOf(matcher.group(lastGroup - 4))
        );
        challenge.setConditionsJson(matcher.group(lastGroup - 3));
        challenge.setExclusionGroup(
            parseNullableSqlString(matcher.group(lastGroup - 2))
        );
        challenge.setEnabled(
            Boolean.parseBoolean(matcher.group(lastGroup - 1))
        );
        challenge.setSchemaVersion(
            Integer.parseInt(matcher.group(lastGroup))
        );

        return challenge;
    }

    /**
     * One production migration seeding the catalogue, with the row shape it was written against.
     *
     * @param resource      classpath location of the migration
     * @param rowPattern    pattern extracting one challenge row from it
     * @param categoryGroup capture group holding the category in that shape
     */
    private record CatalogueMigration(
        String resource,
        Pattern rowPattern,
        int categoryGroup
    ) {
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
     * Reads one production challenge migration from the classpath.
     *
     * @param resource classpath location of the migration
     * @return complete migration content
     * @throws IOException when the resource is missing or unreadable
     */
    private String readCatalogueMigration(String resource) throws IOException {
        ClassLoader classLoader = Thread.currentThread()
            .getContextClassLoader();

        try (InputStream inputStream =
                 classLoader.getResourceAsStream(resource)) {
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
