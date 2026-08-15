package io.github.thomashtn.valorant.tracker.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerAdminResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerCreateRequest;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerDeletionResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerUpdateRequest;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.model.PlayerDeletionOutcome;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.shared.exception.ConflictException;
import io.github.thomashtn.valorant.tracker.synchronization.repository.PlayerSeasonSynchronizationRepository;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationPlayerResultRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PlayerAdminService}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerAdminServiceTest {

    /**
     * Mocked tracked player repository.
     */
    @Mock
    private PlayerRepository playerRepository;

    /**
     * Mocked player match repository.
     */
    @Mock
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Mocked synchronization player result repository.
     */
    @Mock
    private SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Mocked player season synchronization repository.
     */
    @Mock
    private PlayerSeasonSynchronizationRepository seasonSynchronizationRepository;

    /**
     * Mocked campaign contribution resolver.
     */
    @Mock
    private PlayerCampaignContributionResolver contributionResolver;

    /**
     * Service under test.
     */
    private PlayerAdminService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new PlayerAdminService(
            playerRepository,
            playerMatchRepository,
            playerResultRepository,
            seasonSynchronizationRepository,
            contributionResolver
        );
    }

    /**
     * Verifies that the administration listing keeps archived players.
     *
     * <p>They are hidden from the public listing, and this screen is the only place offering to
     * restore one.
     */
    @Test
    void shouldListArchivedPlayersToo() {
        Player archived = player(7L, "Natank", "EUW");
        archived.setStatus(PlayerStatus.ARCHIVED);

        when(playerRepository.findAllByOrderByIdAsc()).thenReturn(List.of(archived));
        when(contributionResolver.hasContributed(7L)).thenReturn(true);

        List<PlayerAdminResponse> players = service.findAll();

        assertThat(players).singleElement().satisfies(response -> {
            assertThat(response.status()).isEqualTo(PlayerStatus.ARCHIVED);
            assertThat(response.hasCampaignContribution()).isTrue();
        });
    }

    /**
     * Verifies that a created player is stored without a Riot PUUID.
     */
    @Test
    void shouldCreateAPlayerWithoutResolvingItsRiotAccount() {
        when(playerRepository.existsByGameNameIgnoreCaseAndTagLineIgnoreCase("Jett", "EUW"))
            .thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
            Player saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        PlayerAdminResponse response = service.create(new PlayerCreateRequest(
            "Jett", "EUW", "Jett", null, PlayerStatus.ACTIVE
        ));

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.riotPuuid()).isNull();
        assertThat(response.status()).isEqualTo(PlayerStatus.ACTIVE);
    }

    /**
     * Verifies that the same Riot identity cannot be tracked twice, whatever its casing.
     */
    @Test
    void shouldRefuseADuplicateRiotIdentity() {
        when(playerRepository.existsByGameNameIgnoreCaseAndTagLineIgnoreCase("jett", "euw"))
            .thenReturn(true);

        assertThatThrownBy(() -> service.create(new PlayerCreateRequest(
            "jett", "euw", "Jett", null, PlayerStatus.ACTIVE
        )))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already tracked");

        verify(playerRepository, never()).save(any(Player.class));
    }

    /**
     * Verifies that pointing a player at another Riot account drops the stored PUUID.
     *
     * <p>Keeping it would go on importing the previous account's matches under the new name.
     */
    @Test
    void shouldClearThePuuidWhenTheRiotIdentityChanges() {
        Player existing = player(3L, "Jett", "EUW");
        existing.setRiotPuuid("old-puuid");

        when(playerRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(playerRepository.existsByGameNameIgnoreCaseAndTagLineIgnoreCase("Sage", "EUW"))
            .thenReturn(false);
        when(playerRepository.save(existing)).thenReturn(existing);

        service.update(3L, new PlayerUpdateRequest("Sage", "EUW", "Sage", null));

        assertThat(existing.getRiotPuuid()).isNull();
        assertThat(existing.getGameName()).isEqualTo("Sage");
    }

    /**
     * Verifies that renaming only the display name keeps the resolved Riot account.
     */
    @Test
    void shouldKeepThePuuidWhenOnlyTheDisplayNameChanges() {
        Player existing = player(3L, "Jett", "EUW");
        existing.setRiotPuuid("known-puuid");

        when(playerRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(playerRepository.save(existing)).thenReturn(existing);

        service.update(3L, new PlayerUpdateRequest("Jett", "EUW", "The Duelist", "jett.png"));

        assertThat(existing.getRiotPuuid()).isEqualTo("known-puuid");
        assertThat(existing.getDisplayName()).isEqualTo("The Duelist");
    }

    /**
     * Verifies that an update cannot steal another player's Riot identity.
     */
    @Test
    void shouldRefuseAnUpdateTakingAnotherPlayersRiotIdentity() {
        Player existing = player(3L, "Jett", "EUW");

        when(playerRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(playerRepository.existsByGameNameIgnoreCaseAndTagLineIgnoreCase("Sage", "EUW"))
            .thenReturn(true);

        assertThatThrownBy(() -> service.update(
            3L, new PlayerUpdateRequest("Sage", "EUW", "Sage", null)
        ))
            .isInstanceOf(ConflictException.class);
    }

    /**
     * Verifies that an unknown identifier is reported as a missing resource.
     */
    @Test
    void shouldRejectAnUnknownPlayer() {
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(404L, PlayerStatus.INACTIVE))
            .isInstanceOf(PlayerNotFoundException.class);
    }

    /**
     * Verifies that an archived player is restored through the status route.
     */
    @Test
    void shouldRestoreAnArchivedPlayer() {
        Player archived = player(7L, "Natank", "EUW");
        archived.setStatus(PlayerStatus.ARCHIVED);

        when(playerRepository.findById(7L)).thenReturn(Optional.of(archived));
        when(playerRepository.save(archived)).thenReturn(archived);

        PlayerAdminResponse response = service.changeStatus(7L, PlayerStatus.ACTIVE);

        assertThat(response.status()).isEqualTo(PlayerStatus.ACTIVE);
    }

    /**
     * Verifies that a player nothing depends on is deleted along with its synchronization traces.
     */
    @Test
    void shouldDeleteAPlayerThatNeverFoughtABoss() {
        Player fresh = player(9L, "Jett", "EUW");

        when(playerRepository.findById(9L)).thenReturn(Optional.of(fresh));
        when(contributionResolver.hasContributed(9L)).thenReturn(false);

        PlayerDeletionResponse response = service.removeFromRoster(9L);

        assertThat(response.outcome()).isEqualTo(PlayerDeletionOutcome.DELETED);

        verify(playerMatchRepository).deleteAll(List.of());
        verify(playerResultRepository).deleteAll(List.of());
        verify(seasonSynchronizationRepository).deleteAll(List.of());
        verify(playerRepository).delete(fresh);
    }

    /**
     * Verifies that a player finalized weeks depend on is archived rather than deleted.
     *
     * <p>Deleting it would leave a closed week crediting a boss kill to a row that no longer
     * exists, and those weeks are immutable.
     */
    @Test
    void shouldArchiveAPlayerThatFoughtABoss() {
        Player veteran = player(3L, "Jett", "EUW");

        when(playerRepository.findById(3L)).thenReturn(Optional.of(veteran));
        when(contributionResolver.hasContributed(3L)).thenReturn(true);
        when(playerRepository.save(veteran)).thenReturn(veteran);

        PlayerDeletionResponse response = service.removeFromRoster(3L);

        assertThat(response.outcome()).isEqualTo(PlayerDeletionOutcome.ARCHIVED);
        assertThat(veteran.getStatus()).isEqualTo(PlayerStatus.ARCHIVED);

        verify(playerRepository, never()).delete(any(Player.class));
        verifyNoInteractions(
            playerMatchRepository,
            playerResultRepository,
            seasonSynchronizationRepository
        );
    }

    /**
     * Creates a tracked player.
     *
     * @param id       internal identifier
     * @param gameName Riot game name
     * @param tagLine  Riot tag line
     * @return the player
     */
    private static Player player(Long id, String gameName, String tagLine) {
        Player player = new Player();

        player.setId(id);
        player.setGameName(gameName);
        player.setTagLine(tagLine);
        player.setDisplayName(gameName);
        player.setStatus(PlayerStatus.ACTIVE);

        return player;
    }
}
