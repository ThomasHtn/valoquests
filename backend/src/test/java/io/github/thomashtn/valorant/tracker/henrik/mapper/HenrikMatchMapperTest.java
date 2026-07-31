package io.github.thomashtn.valorant.tracker.henrik.mapper;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HenrikMatchMapper}, focused on game mode resolution.
 *
 * <p>Riot regularly ships new queues without Henrik changing its contract, so an unrecognized queue
 * silently degrades every downstream statistic. These tests pin the identifiers currently observed
 * in production data as well as the fallback order used when the canonical slug is missing.
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
     * Verifies that Skirmish and Escalation are treated as distinct modes.
     *
     * <p>Riot ships them as two separate game mode assets. They were previously merged, which made
     * every 2v2 match count towards Escalation challenges.
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
     * <p>Henrik returns {@code {"id": "", "name": "Custom Game", "mode_type": "Skirmish"}} for a
     * custom match played with the Skirmish ruleset. Reading the mode type would file it under
     * Skirmish and inflate that mode's history with matches that were never queued for it.
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
     * <p>Riot declines the mode into limited variants such as Skirmish: Ascension, whose exact slug
     * is not known in advance.
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
     * <p>Competitive, Unrated, Premier, Custom and New Map all report it, so guessing would
     * misattribute matches to a mode challenges filter on.
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
            new HenrikMatchPlayer.HenrikDamage(3_200, 2_800)
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
            new HenrikMatchPlayer.HenrikDamage(3_200, 2_800)
        );

        assertThat(result.getAcs()).isNull();
        assertThat(result.getAdr()).isNull();
    }

    /**
     * Verifies that ADR stays absent when Henrik does not report the damage breakdown.
     *
     * <p>Henrik omits it for Skirmish. The persisted total falls back to zero because the column is
     * not nullable, so deriving an average would record a zero that silently drags down the
     * player's statistics instead of being excluded from them.
     */
    @Test
    void shouldNotComputeAdrWhenDamageIsNotReported() {
        PlayerMatch result = mapPlayerMatch(GameMode.SKIRMISH, null);

        assertThat(result.getDamageDealt()).isZero();
        assertThat(result.getAdr()).isNull();
        assertThat(result.getAcs())
            .isEqualByComparingTo(new BigDecimal("250.00"));
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
            new HenrikMatchPlayer.HenrikDamage(3_200, 2_800)
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
            "Psilonnix",
            "EUW",
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
            new HenrikMatchPlayer.HenrikTier(21, "Immortal 1")
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
