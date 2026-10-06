package io.github.thomashtn.valoquests.match.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link HenrikMatchMapper}, focused on game mode resolution.
 *
 * <p>An unrecognized queue silently degrades every statistic, so these tests pin the observed slugs and fallbacks.
 */
class HenrikMatchMapperTest {

    /**
     * Mapper under test.
     */
    private HenrikMatchMapper mapper;

    /**
     * Season the mapped matches belong to.
     */
    private Season season;

    /**
     * Creates a fresh mapper before each test.
     */
    @BeforeEach
    void setUp() {
        mapper = new HenrikMatchMapper();
        season = new Season();
    }

    /**
     * Verifies that every queue slug observed in production data resolves to its game mode.
     *
     * @param queueId raw Henrik queue slug
     * @param expected expected persisted game mode
     */
    @ParameterizedTest
    @CsvSource({
        "competitive, COMPETITIVE",
        "unrated, UNRATED",
        "swiftplay, SWIFTPLAY",
        "newmap, NEW_MAP",
        "spikerush, SPIKE_RUSH",
        "deathmatch, DEATHMATCH",
        "hurm, TEAM_DEATHMATCH",
        "ggteam, ESCALATION",
        "skirmish_2v2, SKIRMISH",
        "premier, PREMIER",
        "custom, CUSTOM"
    })
    void shouldResolveGameModeFromQueueSlug(
        String queueId,
        GameMode expected
    ) {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                queueId,
                null,
                null
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(expected);
    }

    /**
     * Verifies that a mode resolved from the canonical queue slug is attributed to
     * {@link GameModeSource#PROVIDED}.
     */
    @Test
    void shouldAttributeQueueSlugResolutionToProvided() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue("deathmatch", null, null)),
            season
        );

        assertThat(result.getGameModeSource()).isEqualTo(GameModeSource.PROVIDED);
    }

    /**
     * Verifies that a mode resolved from the display name fallback is attributed to
     * {@link GameModeSource#INFERRED}, not {@link GameModeSource#PROVIDED}.
     */
    @Test
    void shouldAttributeQueueNameFallbackToInferred() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(null, "Skirmish", null)),
            season
        );

        assertThat(result.getGameModeSource()).isEqualTo(GameModeSource.INFERRED);
    }

    /**
     * Verifies that a mode resolved from the mode-type fallback is attributed to
     * {@link GameModeSource#INFERRED}.
     */
    @Test
    void shouldAttributeModeTypeFallbackToInferred() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(null, null, "Team Deathmatch")),
            season
        );

        assertThat(result.getGameModeSource()).isEqualTo(GameModeSource.INFERRED);
    }

    /**
     * Verifies that an unresolved queue is attributed to {@link GameModeSource#UNKNOWN}.
     */
    @Test
    void shouldAttributeUnresolvedQueueToUnknown() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue("mystery", "Mystery", "Mystery")),
            season
        );

        assertThat(result.getGameModeSource()).isEqualTo(GameModeSource.UNKNOWN);
    }

    /**
     * Verifies that a missing queue is attributed to {@link GameModeSource#UNKNOWN}.
     */
    @Test
    void shouldAttributeMissingQueueToUnknown() {
        ValorantMatch result = mapper.toValorantMatch(matchWithQueue(null), season);

        assertThat(result.getGameModeSource()).isEqualTo(GameModeSource.UNKNOWN);
    }

    /**
     * Verifies that Skirmish and Escalation are treated as distinct modes.
     *
     * <p>Merging them once made every 2v2 match count towards Escalation challenges.
     */
    @Test
    void shouldNotConfuseSkirmishWithEscalation() {
        ValorantMatch skirmish = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "skirmish_2v2",
                null,
                null
            )),
            season
        );
        ValorantMatch escalation = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "escalation",
                null,
                null
            )),
            season
        );

        assertThat(skirmish.getGameMode()).isEqualTo(GameMode.SKIRMISH);
        assertThat(escalation.getGameMode())
            .isEqualTo(GameMode.ESCALATION);
    }

    /**
     * Verifies that a custom game is classified by its queue, not by the ruleset it uses.
     *
     * <p>Henrik reports a Skirmish-ruleset custom game with mode type {@code Skirmish}; reading it would
     * inflate that mode's history.
     */
    @Test
    void shouldClassifyCustomGameByQueueRatherThanRuleset() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "",
                "Custom Game",
                "Skirmish"
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.CUSTOM);
    }

    /**
     * Verifies that the new-map queue is not misread as its map name.
     *
     * <p>Henrik puts the map name, not a mode label, in the display name of that queue.
     */
    @Test
    void shouldClassifyNewMapQueueDespiteMapNameAsDisplayName() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "newmap",
                "Summit",
                "Swiftplay"
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.NEW_MAP);
    }

    /**
     * Verifies that the Skirmish queue resolves although Henrik reports no display name for it.
     */
    @Test
    void shouldClassifySkirmishQueueWithoutDisplayName() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "skirmish_2v2",
                null,
                "Skirmish"
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.SKIRMISH);
    }

    /**
     * Verifies that an unknown Skirmish variant still resolves to Skirmish.
     *
     * <p>Riot ships limited variants such as Skirmish: Ascension whose slug is not known in advance.
     */
    @Test
    void shouldResolveUnknownSkirmishVariant() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "skirmish_ascension",
                null,
                null
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.SKIRMISH);
    }

    /**
     * Verifies that a blank queue slug falls back to the display name.
     *
     * <p>Henrik occasionally returns an empty slug for an otherwise valid match.
     *
     * @param queueId blank or missing queue slug
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldFallBackToQueueNameWhenSlugIsBlank(String queueId) {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                queueId,
                "Skirmish",
                null
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.SKIRMISH);
    }

    /**
     * Verifies that the Riot game mode asset name is used as a last resort.
     *
     * <p>This is what lets a queue renamed by Riot remain categorized without a code change.
     */
    @Test
    void shouldFallBackToModeTypeWhenSlugAndNameAreUnknown() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                null,
                null,
                "Team Deathmatch"
            )),
            season
        );

        assertThat(result.getGameMode())
            .isEqualTo(GameMode.TEAM_DEATHMATCH);
    }

    /**
     * Verifies that the ambiguous {@code Standard} mode type is never resolved.
     *
     * <p>Competitive, Unrated, Premier, Custom and New Map all report it, so guessing would misattribute matches.
     */
    @Test
    void shouldNotResolveAmbiguousStandardModeType() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                null,
                null,
                "Standard"
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.OTHER);
    }

    /**
     * Verifies that an unrecognized queue falls back to the catch-all mode.
     */
    @Test
    void shouldFallBackToOtherForUnknownQueue() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "mode-riot-has-not-shipped-yet",
                "Mystery Mode",
                "Mystery"
            )),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.OTHER);
    }

    /**
     * Verifies that a missing queue falls back to the catch-all mode.
     */
    @Test
    void shouldFallBackToOtherWhenQueueIsMissing() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(null),
            season
        );

        assertThat(result.getGameMode()).isEqualTo(GameMode.OTHER);
    }

    /**
     * Verifies that the raw queue slug is preserved alongside the normalized mode.
     *
     * <p>It is the only evidence available to re-categorize matches once a new mode is supported.
     */
    @Test
    void shouldPreserveRawQueueSlug() {
        ValorantMatch result = mapper.toValorantMatch(
            matchWithQueue(new HenrikMatchMetadata.HenrikQueue(
                "skirmish_2v2",
                null,
                null
            )),
            season
        );

        assertThat(result.getQueueId()).isEqualTo("skirmish_2v2");
    }

    /**
     * Verifies that round averages are computed for the round-based Skirmish mode.
     */
    @Test
    void shouldComputeRoundAveragesForSkirmish() {
        PlayerMatch result = mapPlayerMatch(
            GameMode.SKIRMISH,
            new HenrikMatchPlayer.HenrikDamage(3_200)
        );

        assertThat(result.getRoundsPlayed()).isEqualTo(16);
        assertThat(result.getAcs())
            .isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.getAdr())
            .isEqualByComparingTo(new BigDecimal("200.00"));
    }

    /**
     * Verifies that round averages stay absent for modes without scored rounds.
     */
    @Test
    void shouldNotComputeRoundAveragesForDeathmatch() {
        PlayerMatch result = mapPlayerMatch(
            GameMode.DEATHMATCH,
            new HenrikMatchPlayer.HenrikDamage(3_200)
        );

        assertThat(result.getAcs()).isNull();
        assertThat(result.getAdr()).isNull();
    }

    /**
     * Verifies that ADR stays absent when Henrik does not report the damage breakdown.
     *
     * <p>Henrik omits it for Skirmish and the stored total falls back to zero, which would drag the average down.
     */
    @Test
    void shouldNotComputeAdrWhenDamageIsNotReported() {
        PlayerMatch result = mapPlayerMatch(GameMode.SKIRMISH, null);

        assertThat(result.getDamageDealt()).isZero();
        assertThat(result.getAdr()).isNull();
        assertThat(result.getAcs())
            .isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Leaves ADR unset when Henrik reports a zero damage total, as it does for Skirmish")
    void shouldNotComputeAdrFromAZeroDamageTotal() {
        PlayerMatch result = mapPlayerMatch(GameMode.SKIRMISH, new HenrikMatchPlayer.HenrikDamage(0));

        assertThat(result.getDamageDealt()).isZero();
        assertThat(result.getAdr()).isNull();
    }

    @Test
    @DisplayName("Records a draw when neither team won and the rounds are level")
    void shouldRecordADrawOnLevelRounds() {
        assertThat(resultOf(false, 14, 14)).isEqualTo(MatchResult.DRAW);
    }

    @Test
    @DisplayName("Records a loss, not a draw, on level rounds when the opposing team won")
    void shouldRecordALossOnLevelRoundsWhenTheOpponentWon() {
        assertThat(resultOf(false, true, 13, 13)).isEqualTo(MatchResult.LOSS);
    }

    @Test
    @DisplayName("Records a loss when the player's team did not win and trails on rounds")
    void shouldRecordALossWhenTrailingOnRounds() {
        assertThat(resultOf(false, 6, 13)).isEqualTo(MatchResult.LOSS);
    }

    /**
     * Maps the result of a match whose opposing team did not win and reports the opposite rounds.
     *
     * @param won        flag Henrik reports for the player's team
     * @param roundsWon  rounds the player's team won
     * @param roundsLost rounds the player's team lost
     * @return mapped result
     */
    private MatchResult resultOf(boolean won, int roundsWon, int roundsLost) {
        return resultOf(won, false, roundsWon, roundsLost);
    }

    /**
     * Maps the result of a match whose two teams report the given flags and opposite rounds.
     *
     * @param won         flag Henrik reports for the player's team
     * @param opponentWon flag Henrik reports for the opposing team
     * @param roundsWon   rounds the player's team won
     * @param roundsLost  rounds the player's team lost
     * @return mapped result
     */
    private MatchResult resultOf(boolean won, boolean opponentWon, int roundsWon, int roundsLost) {
        HenrikMatchData base = matchWithQueue(null);
        HenrikMatchData source = new HenrikMatchData(base.metadata(), base.players(), List.of(
            new HenrikMatchTeam("Red", won, new HenrikMatchTeam.HenrikRounds(roundsWon, roundsLost)),
            new HenrikMatchTeam("Blue", opponentWon, new HenrikMatchTeam.HenrikRounds(roundsLost, roundsWon))
        ));
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(GameMode.COMPETITIVE);

        return mapper.toPlayerMatch(source, source.players().getFirst(), new Player(), match).getResult();
    }

    /**
     * Maps the tracked player's statistics for a match played in the supplied mode.
     *
     * @param gameMode mode of the persisted match
     * @param damage damage breakdown returned by Henrik, possibly {@code null}
     * @return mapped player statistics
     */
    private PlayerMatch mapPlayerMatch(
        GameMode gameMode,
        HenrikMatchPlayer.HenrikDamage damage
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);

        HenrikMatchData source = matchWithQueue(null, damage);

        return mapper.toPlayerMatch(
            source,
            source.players().getFirst(),
            new Player(),
            match
        );
    }

    /**
     * Creates a minimal Henrik match exposing the supplied queue and a full damage breakdown.
     *
     * @param queue queue returned by Henrik, possibly {@code null}
     * @return external Henrik match
     */
    private HenrikMatchData matchWithQueue(
        HenrikMatchMetadata.HenrikQueue queue
    ) {
        return matchWithQueue(
            queue,
            new HenrikMatchPlayer.HenrikDamage(3_200)
        );
    }

    /**
     * Creates a minimal Henrik match exposing the supplied queue and damage breakdown.
     *
     * @param queue queue returned by Henrik, possibly {@code null}
     * @param damage damage breakdown returned by Henrik, possibly {@code null}
     * @return external Henrik match
     */
    private HenrikMatchData matchWithQueue(
        HenrikMatchMetadata.HenrikQueue queue,
        HenrikMatchPlayer.HenrikDamage damage
    ) {
        HenrikMatchMetadata metadata = new HenrikMatchMetadata(
            "match-123",
            new HenrikMatchMetadata.HenrikMap("map-123", "Skirmish E"),
            330_000L,
            Instant.parse("2026-07-23T18:51:01Z"),
            true,
            queue,
            new HenrikMatchMetadata.HenrikSeason(
                "season-123",
                "V26 Act 4"
            )
        );

        HenrikMatchPlayer player = new HenrikMatchPlayer(
            "puuid-123",
            "Red",
            new HenrikMatchPlayer.HenrikAgent("agent-123", "Jett"),
            new HenrikMatchPlayer.HenrikPlayerStats(
                4_000,
                20,
                12,
                3,
                10,
                25,
                2,
                damage
            ),
            new HenrikMatchPlayer.HenrikTier("Immortal 1")
        );

        List<HenrikMatchTeam> teams = List.of(
            new HenrikMatchTeam(
                "Red",
                true,
                new HenrikMatchTeam.HenrikRounds(10, 6)
            ),
            new HenrikMatchTeam(
                "Blue",
                false,
                new HenrikMatchTeam.HenrikRounds(6, 10)
            )
        );

        return new HenrikMatchData(metadata, List.of(player), teams);
    }
}
