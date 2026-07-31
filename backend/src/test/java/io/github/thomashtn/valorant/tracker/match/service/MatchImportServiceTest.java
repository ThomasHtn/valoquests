package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMatchMapper;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchImportResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MatchImportService}, focused on the game-mode filter.
 *
 * <p>The filter decides what ever reaches the match tables, and therefore what challenges can count.
 * A mode wrongly let through pollutes the statistics; a mode wrongly rejected is lost until a full
 * season is re-imported.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchImportServiceTest {

    /**
     * Riot identifier of the tracked player.
     */
    private static final String PUUID = "puuid-1";

    /**
     * Henrik identifier of the imported season.
     */
    private static final String SEASON_ID = "season-1";

    @Mock
    private ValorantMatchRepository matchRepository;

    @Mock
    private PlayerMatchRepository playerMatchRepository;

    @Mock
    private SeasonResolutionService seasonResolutionService;

    /**
     * Service under test, driven by the production mapper.
     */
    private MatchImportService service;

    /**
     * Tracked player used by every test.
     */
    private Player player;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new MatchImportService(
            matchRepository,
            playerMatchRepository,
            seasonResolutionService,
            new HenrikMatchMapper()
        );

        player = new Player();
        player.setId(1L);
        player.setRiotPuuid(PUUID);

        when(seasonResolutionService.resolve(any())).thenReturn(new Season());
        when(matchRepository.findByExternalMatchId(any())).thenReturn(Optional.empty());
        when(matchRepository.save(any(ValorantMatch.class)))
            .thenAnswer(invocation -> {
                ValorantMatch match = invocation.getArgument(0);
                match.setId(100L);
                return match;
            });
        when(playerMatchRepository.existsByPlayerIdAndMatchId(any(), any())).thenReturn(false);
    }

    /**
     * Verifies that a mode the tracker follows is stored.
     *
     * @param queueId raw Henrik queue slug of an imported mode
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "competitive", "unrated", "premier", "deathmatch", "spikerush", "skirmish_2v2", "hurm"
    })
    void shouldImportFollowedGameModes(String queueId) {
        MatchImportResult result = importOne(queueId);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(playerMatchRepository).save(any(PlayerMatch.class));
    }

    /**
     * Verifies that an ignored mode never creates a match row.
     *
     * <p>Rejecting it before the lookup matters: a stored match would still be counted by the
     * "matches played" challenges filtered on {@code ANY}.
     *
     * @param queueId raw Henrik queue slug of an ignored mode
     */
    @ParameterizedTest
    @ValueSource(strings = {"swiftplay", "newmap", "ggteam", "custom"})
    void shouldSkipIgnoredGameModes(String queueId) {
        MatchImportResult result = importOne(queueId);

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.imported()).isZero();
        assertThat(result.alreadyKnown()).isZero();
        assertThat(result.rejected()).isZero();

        verify(matchRepository, never()).save(any(ValorantMatch.class));
        verify(playerMatchRepository, never()).save(any(PlayerMatch.class));
        verifyNoInteractions(seasonResolutionService);
    }

    /**
     * Verifies that a queue this application cannot classify is stored with its raw slug.
     *
     * <p>Henrik lags behind Riot releases, so dropping an unknown queue would lose matches that a
     * later reclassification could have recovered from the persisted slug.
     */
    @Test
    void shouldImportAnUnclassifiedQueueAndPreserveItsRawSlug() {
        MatchImportResult result = importOne("valorant_royale");

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isZero();

        ArgumentCaptor<ValorantMatch> saved = ArgumentCaptor.forClass(ValorantMatch.class);
        verify(matchRepository).save(saved.capture());

        assertThat(saved.getValue().getGameMode()).isEqualTo(GameMode.OTHER);
        assertThat(saved.getValue().getQueueId()).isEqualTo("valorant_royale");
    }

    /**
     * Verifies that an already stored association is reported rather than duplicated.
     */
    @Test
    void shouldReportAnAlreadyStoredMatch() {
        ValorantMatch existing = new ValorantMatch();
        existing.setId(100L);

        when(matchRepository.findByExternalMatchId("match-1"))
            .thenReturn(Optional.of(existing));
        when(playerMatchRepository.existsByPlayerIdAndMatchId(1L, 100L)).thenReturn(true);

        MatchImportResult result = importOne("competitive");

        assertThat(result.alreadyKnown()).isEqualTo(1);
        assertThat(result.imported()).isZero();
        assertThat(result.knownHistoryReached()).isTrue();
        verify(playerMatchRepository, never()).save(any(PlayerMatch.class));
    }

    /**
     * Verifies that a malformed entry is rejected before the mode is even considered.
     */
    @Test
    void shouldRejectAnIncompleteMatch() {
        HenrikMatchData incomplete = new HenrikMatchData(
            new HenrikMatchMetadata(
                "match-1",
                null,
                null,
                Instant.parse("2026-07-20T18:00:00Z"),
                false,
                new HenrikMatchMetadata.HenrikQueue("competitive", null, null),
                new HenrikMatchMetadata.HenrikSeason(SEASON_ID, "V26 Act 4")
            ),
            List.of(),
            List.of()
        );

        MatchImportResult result = service.importMatchesWithSummary(
            player,
            new HenrikMatchHistoryResponse(200, List.of(incomplete))
        );

        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
    }

    /**
     * Verifies that a match carrying no season identifier is rejected.
     *
     * <p>The season is what scopes the walk, so a match without one cannot be placed in the history.
     * This precondition is locked in because a mode Henrik systematically returns without a season
     * would disappear from the tracker entirely, looking exactly like a mode never played.
     */
    @Test
    void shouldRejectAMatchWithoutASeasonIdentifier() {
        HenrikMatchData seasonless = new HenrikMatchData(
            new HenrikMatchMetadata(
                "match-1",
                new HenrikMatchMetadata.HenrikMap("map-1", "Ascent"),
                1_800_000L,
                Instant.parse("2026-07-20T18:00:00Z"),
                true,
                new HenrikMatchMetadata.HenrikQueue("deathmatch", null, null),
                new HenrikMatchMetadata.HenrikSeason(" ", "V26 Act 4")
            ),
            List.of(),
            List.of()
        );

        MatchImportResult result = service.importMatchesWithSummary(
            player,
            new HenrikMatchHistoryResponse(200, List.of(seasonless))
        );

        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.imported()).isZero();
        verify(playerMatchRepository, never()).save(any(PlayerMatch.class));
    }

    /**
     * Verifies that every counter of a mixed page adds up to the received count.
     */
    @Test
    void shouldCountEveryOutcomeOfAMixedPage() {
        MatchImportResult result = service.importMatchesWithSummary(
            player,
            // Arrays.asList rather than List.of: Henrik does return null entries, and the response
            // record preserves them so they are counted as rejected instead of vanishing.
            new HenrikMatchHistoryResponse(200, Arrays.asList(
                match("match-1", "competitive"),
                match("match-2", "swiftplay"),
                match("match-3", "ggteam"),
                match("match-4", "deathmatch"),
                null
            ))
        );

        assertThat(result.received()).isEqualTo(5);
        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.alreadyKnown()).isZero();
    }

    /**
     * Imports a single match of the given queue.
     */
    private MatchImportResult importOne(String queueId) {
        return service.importMatchesWithSummary(
            player,
            new HenrikMatchHistoryResponse(200, List.of(match("match-1", queueId)))
        );
    }

    /**
     * Creates a completed Henrik match the tracked player took part in.
     */
    private HenrikMatchData match(String matchId, String queueId) {
        return new HenrikMatchData(
            new HenrikMatchMetadata(
                matchId,
                new HenrikMatchMetadata.HenrikMap("map-1", "Ascent"),
                1_800_000L,
                Instant.parse("2026-07-20T18:00:00Z"),
                true,
                new HenrikMatchMetadata.HenrikQueue(queueId, null, null),
                new HenrikMatchMetadata.HenrikSeason(SEASON_ID, "V26 Act 4")
            ),
            List.of(new HenrikMatchPlayer(
                PUUID,
                "Player",
                "EUW",
                "Red",
                new HenrikMatchPlayer.HenrikAgent("agent-1", "Jett"),
                new HenrikMatchPlayer.HenrikPlayerStats(
                    4000, 20, 12, 3, 10, 25, 2,
                    new HenrikMatchPlayer.HenrikDamage(3200, 2800)
                ),
                new HenrikMatchPlayer.HenrikTier(21, "Immortal 1")
            )),
            List.of(
                new HenrikMatchTeam("Red", true, new HenrikMatchTeam.HenrikRounds(13, 7)),
                new HenrikMatchTeam("Blue", false, new HenrikMatchTeam.HenrikRounds(7, 13))
            )
        );
    }
}
