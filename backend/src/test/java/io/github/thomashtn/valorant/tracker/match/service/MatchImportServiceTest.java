package io.github.thomashtn.valorant.tracker.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import io.github.thomashtn.valorant.tracker.match.model.GameModeSource;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

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

    @Mock
    private PlatformTransactionManager transactionManager;

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
            new HenrikMatchMapper(),
            transactionManager
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
        existing.setGameMode(GameMode.COMPETITIVE);
        existing.setGameModeSource(GameModeSource.PROVIDED);

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
     * Verifies that a match under-classified by an earlier synchronization is enriched once a more
     * confident resolution becomes available.
     *
     * <p>Henrik's queue slug is not always populated; a run that only had the display name to work
     * with stores {@link GameModeSource#INFERRED}. A later run that gets the canonical slug must
     * upgrade the stored value rather than leaving it at its first, weaker guess.
     */
    @Test
    void shouldEnrichAnExistingMatchWhenAMoreConfidentSourceResolves() {
        ValorantMatch existing = new ValorantMatch();
        existing.setId(100L);
        existing.setGameMode(GameMode.SKIRMISH);
        existing.setGameModeSource(GameModeSource.INFERRED);

        when(matchRepository.findByExternalMatchId("match-1"))
            .thenReturn(Optional.of(existing));

        importMatchWithQueue("match-1", new HenrikMatchMetadata.HenrikQueue("deathmatch", null, null));

        assertThat(existing.getGameMode()).isEqualTo(GameMode.DEATHMATCH);
        assertThat(existing.getGameModeSource()).isEqualTo(GameModeSource.PROVIDED);
        verify(matchRepository).save(existing);
    }

    /**
     * Verifies that a weaker resolution never downgrades an already stored, more confident one.
     */
    @Test
    void shouldNotDowngradeAProvidedGameModeWithAnInferredOne() {
        ValorantMatch existing = new ValorantMatch();
        existing.setId(100L);
        existing.setGameMode(GameMode.DEATHMATCH);
        existing.setGameModeSource(GameModeSource.PROVIDED);

        when(matchRepository.findByExternalMatchId("match-1"))
            .thenReturn(Optional.of(existing));

        importMatchWithQueue("match-1", new HenrikMatchMetadata.HenrikQueue(null, "Skirmish", null));

        assertThat(existing.getGameMode()).isEqualTo(GameMode.DEATHMATCH);
        assertThat(existing.getGameModeSource()).isEqualTo(GameModeSource.PROVIDED);
        verify(matchRepository, never()).save(existing);
    }

    /**
     * Verifies that a manual correction is never replaced by a value from a synchronization, however
     * confidently that value was resolved.
     */
    @Test
    void shouldNeverReplaceAManualCorrectionWithASynchronizedValue() {
        ValorantMatch existing = new ValorantMatch();
        existing.setId(100L);
        existing.setGameMode(GameMode.CUSTOM);
        existing.setGameModeSource(GameModeSource.MANUALLY_CORRECTED);

        when(matchRepository.findByExternalMatchId("match-1"))
            .thenReturn(Optional.of(existing));

        importMatchWithQueue("match-1", new HenrikMatchMetadata.HenrikQueue("deathmatch", null, null));

        assertThat(existing.getGameMode()).isEqualTo(GameMode.CUSTOM);
        assertThat(existing.getGameModeSource()).isEqualTo(GameModeSource.MANUALLY_CORRECTED);
        verify(matchRepository, never()).save(existing);
    }

    /**
     * Verifies that a freshly created match is never redundantly re-saved by the enrichment step: its
     * just-persisted value already matches what enrichment would resolve.
     */
    @Test
    void shouldNotRedundantlySaveAFreshlyCreatedMatch() {
        importOne("competitive");

        // Once by createMatch, never again by enrichment.
        verify(matchRepository, times(1)).save(any(ValorantMatch.class));
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
     * Verifies that losing the race to create a shared match reuses the winner's row instead of
     * failing the whole page.
     *
     * <p>Two tracked players who both took part in the same match can be synchronized concurrently,
     * or the same player can be caught up manually while a scheduled run is still in progress. Either
     * way, the loser must recover by reusing the row the winner committed, not by propagating the
     * database's unique-constraint violation.
     */
    @Test
    void shouldReuseAMatchCreatedConcurrently() {
        ValorantMatch winnerRow = new ValorantMatch();
        winnerRow.setId(200L);
        winnerRow.setGameMode(GameMode.COMPETITIVE);
        winnerRow.setGameModeSource(GameModeSource.PROVIDED);

        when(matchRepository.findByExternalMatchId("match-1"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(winnerRow));
        when(matchRepository.save(any(ValorantMatch.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate external_match_id"));

        MatchImportResult result = importOne("competitive");

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.rejected()).isZero();

        ArgumentCaptor<PlayerMatch> saved = ArgumentCaptor.forClass(PlayerMatch.class);
        verify(playerMatchRepository).save(saved.capture());
        assertThat(saved.getValue().getMatch()).isSameAs(winnerRow);
    }

    /**
     * Verifies that losing the race to create a player-match association reports it as already known
     * instead of failing the whole page.
     */
    @Test
    void shouldReportAnAssociationCreatedConcurrently() {
        when(playerMatchRepository.save(any(PlayerMatch.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate player_id, match_id"));

        MatchImportResult result = importOne("competitive");

        assertThat(result.imported()).isZero();
        assertThat(result.alreadyKnown()).isEqualTo(1);
        assertThat(result.rejected()).isZero();
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
     * Imports a single match carrying the given raw queue payload.
     */
    private MatchImportResult importMatchWithQueue(
        String matchId,
        HenrikMatchMetadata.HenrikQueue queue
    ) {
        return service.importMatchesWithSummary(
            player,
            new HenrikMatchHistoryResponse(200, List.of(match(matchId, queue)))
        );
    }

    /**
     * Creates a completed Henrik match the tracked player took part in.
     */
    private HenrikMatchData match(String matchId, String queueId) {
        return match(matchId, new HenrikMatchMetadata.HenrikQueue(queueId, null, null));
    }

    /**
     * Creates a completed Henrik match the tracked player took part in.
     */
    private HenrikMatchData match(String matchId, HenrikMatchMetadata.HenrikQueue queue) {
        return new HenrikMatchData(
            new HenrikMatchMetadata(
                matchId,
                new HenrikMatchMetadata.HenrikMap("map-1", "Ascent"),
                1_800_000L,
                Instant.parse("2026-07-20T18:00:00Z"),
                true,
                queue,
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
