package io.github.thomashtn.valorant.tracker.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.synchronization.entity.PlayerSeasonSynchronization;
import io.github.thomashtn.valorant.tracker.synchronization.repository.PlayerSeasonSynchronizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SeasonSynchronizationStateService}.
 */
@ExtendWith(MockitoExtension.class)
class SeasonSynchronizationStateServiceTest {

    /**
     * Fixed completion time used by the tests.
     */
    private static final Instant COMPLETED_AT =
        Instant.parse("2026-07-25T09:00:00Z");

    /**
     * Henrik identifier of the tracked season.
     */
    private static final String SEASON_EXTERNAL_ID = "season-1";

    @Mock
    private PlayerSeasonSynchronizationRepository stateRepository;

    /**
     * Service under test.
     */
    private SeasonSynchronizationStateService service;

    /**
     * Player whose seasons are tracked.
     */
    private Player player;

    /**
     * Season being walked.
     */
    private Season season;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new SeasonSynchronizationStateService(
            stateRepository,
            Clock.fixed(COMPLETED_AT, ZoneOffset.UTC)
        );

        player = new Player();
        player.setId(1L);

        season = new Season();
        season.setId(7L);
        season.setExternalId(SEASON_EXTERNAL_ID);
    }

    /**
     * Verifies that starting an unknown season records it as unfinished.
     *
     * <p>The row must exist before the first page is imported: it is what tells the next run the
     * stored history may have holes.
     */
    @Test
    void shouldRecordANewSeasonAsIncomplete() {
        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.empty());

        Long seasonId = service.startSeason(player, season);

        assertThat(seasonId).isEqualTo(7L);

        ArgumentCaptor<PlayerSeasonSynchronization> saved =
            ArgumentCaptor.forClass(PlayerSeasonSynchronization.class);
        verify(stateRepository).save(saved.capture());

        assertThat(saved.getValue().isComplete()).isFalse();
        assertThat(saved.getValue().getCompletedAt()).isNull();
        assertThat(saved.getValue().getPlayer()).isSameAs(player);
        assertThat(saved.getValue().getSeason()).isSameAs(season);
    }

    /**
     * Verifies that starting an already tracked season does not reset its flag.
     *
     * <p>Resetting would force a full re-walk of every completed season on every run.
     */
    @Test
    void shouldPreserveTheStateOfAnAlreadyTrackedSeason() {
        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.of(state(true)));

        Long seasonId = service.startSeason(player, season);

        assertThat(seasonId).isEqualTo(7L);
        verify(stateRepository, never()).save(any());
    }

    /**
     * Verifies that completing a season records the flag and its instant.
     */
    @Test
    void shouldMarkASeasonComplete() {
        PlayerSeasonSynchronization state = state(false);
        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.of(state));

        service.markSeasonComplete(1L, 7L);

        assertThat(state.isComplete()).isTrue();
        assertThat(state.getCompletedAt()).isEqualTo(COMPLETED_AT);
        verify(stateRepository).save(state);
    }

    /**
     * Verifies that re-marking a complete season keeps its original instant.
     *
     * <p>Every run of a completed season crosses its boundary again, so the completion instant must
     * keep reporting when the history was actually secured rather than the last run.
     */
    @Test
    void shouldNotRewriteAnAlreadyCompleteSeason() {
        PlayerSeasonSynchronization state = state(true);
        Instant originalInstant = Instant.parse("2026-07-01T00:00:00Z");
        state.setCompletedAt(originalInstant);

        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.of(state));

        service.markSeasonComplete(1L, 7L);

        assertThat(state.getCompletedAt()).isEqualTo(originalInstant);
        verify(stateRepository, never()).save(any());
    }

    /**
     * Verifies that completing an untracked season is a no-op rather than a failure.
     */
    @Test
    void shouldIgnoreCompletionOfAnUntrackedSeason() {
        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.empty());

        service.markSeasonComplete(1L, 7L);

        verify(stateRepository, never()).save(any());
    }

    /**
     * Verifies that only a complete season allows an early stop.
     */
    @Test
    void shouldReportSeasonCompletion() {
        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.of(state(true)));

        assertThat(service.isComplete(1L, 7L)).isTrue();
    }

    /**
     * Verifies that an untracked season never allows an early stop.
     */
    @Test
    void shouldNotReportAnUntrackedSeasonAsComplete() {
        when(stateRepository.findByPlayerIdAndSeasonId(1L, 7L))
            .thenReturn(Optional.empty());

        assertThat(service.isComplete(1L, 7L)).isFalse();
    }

    /**
     * Verifies that an unfinished season is offered for resumption.
     */
    @Test
    void shouldResumeAnUnfinishedSeason() {
        when(stateRepository.findByPlayerIdAndSeasonExternalId(1L, SEASON_EXTERNAL_ID))
            .thenReturn(Optional.of(state(false)));

        assertThat(service.findResumableSeasonId(1L, SEASON_EXTERNAL_ID))
            .contains(7L);
    }

    /**
     * Verifies that a completed season is never walked again.
     */
    @Test
    void shouldNotResumeACompleteSeason() {
        when(stateRepository.findByPlayerIdAndSeasonExternalId(1L, SEASON_EXTERNAL_ID))
            .thenReturn(Optional.of(state(true)));

        assertThat(service.findResumableSeasonId(1L, SEASON_EXTERNAL_ID))
            .isEmpty();
    }

    /**
     * Verifies that a season the player never targeted is left alone.
     *
     * <p>This is what bounds a first run on an empty database to the current season instead of
     * walking the player's whole Valorant history.
     */
    @Test
    void shouldNotResumeANeverTargetedSeason() {
        when(stateRepository.findByPlayerIdAndSeasonExternalId(1L, "season-0"))
            .thenReturn(Optional.empty());

        assertThat(service.findResumableSeasonId(1L, "season-0")).isEmpty();
    }

    /**
     * Creates a stored state for the tracked season.
     */
    private PlayerSeasonSynchronization state(boolean complete) {
        PlayerSeasonSynchronization state = new PlayerSeasonSynchronization();
        state.setPlayer(player);
        state.setSeason(season);
        state.setComplete(complete);
        return state;
    }
}
