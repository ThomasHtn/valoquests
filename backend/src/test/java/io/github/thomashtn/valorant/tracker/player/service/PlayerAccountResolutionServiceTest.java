package io.github.thomashtn.valorant.tracker.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikAccountClient;
import io.github.thomashtn.valorant.tracker.henrik.model.HenrikAccount;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerAccountConflictException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PlayerAccountResolutionService}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerAccountResolutionServiceTest {

    /**
     * Mocked Henrik account client.
     */
    @Mock
    private HenrikAccountClient accountClient;

    /**
     * Mocked player repository.
     */
    @Mock
    private PlayerRepository playerRepository;

    /**
     * Service under test.
     */
    private PlayerAccountResolutionService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new PlayerAccountResolutionService(
            accountClient,
            playerRepository
        );
    }

    /**
     * Verifies that a null player is rejected immediately.
     */
    @Test
    void shouldRejectNullPlayer() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> service.resolvePuuid(null))
            .withMessage("Player must not be null");

        verifyNoInteractions(
            accountClient,
            playerRepository
        );
    }

    /**
     * Verifies that no Henrik request or persistence operation is performed
     * when the player already owns a PUUID.
     */
    @Test
    void shouldReturnPlayerWithoutCallingHenrikWhenPuuidAlreadyExists() {
        Player player = createPlayer();
        player.setRiotPuuid("existing-puuid");

        Player result = service.resolvePuuid(player);

        assertThat(result).isSameAs(player);
        assertThat(result.getRiotPuuid()).isEqualTo("existing-puuid");

        verifyNoInteractions(
            accountClient,
            playerRepository
        );
    }

    /**
     * Verifies that a blank stored PUUID is treated as missing.
     */
    @Test
    void shouldResolvePuuidWhenStoredValueIsBlank() {
        Player player = createPlayer();
        player.setRiotPuuid(" ");

        HenrikAccount account = new HenrikAccount(
            "resolved-puuid",
            "Psilonnix",
            "EUW"
        );

        when(
            accountClient.getAccount(
                "Psilonnix",
                "EUW"
            )
        ).thenReturn(account);

        when(
            playerRepository.existsByRiotPuuid("resolved-puuid")
        ).thenReturn(false);

        when(playerRepository.save(player)).thenReturn(player);

        Player result = service.resolvePuuid(player);

        assertThat(result).isSameAs(player);
        assertThat(result.getRiotPuuid()).isEqualTo("resolved-puuid");

        verify(accountClient).getAccount(
            "Psilonnix",
            "EUW"
        );

        verify(playerRepository)
            .existsByRiotPuuid("resolved-puuid");

        verify(playerRepository).save(player);
    }

    /**
     * Verifies the complete successful resolution flow.
     */
    @Test
    void shouldResolveAndSaveMissingPuuid() {
        Player player = createPlayer();

        HenrikAccount account = new HenrikAccount(
            "resolved-puuid",
            "Psilonnix",
            "EUW"
        );

        when(
            accountClient.getAccount(
                "Psilonnix",
                "EUW"
            )
        ).thenReturn(account);

        when(
            playerRepository.existsByRiotPuuid("resolved-puuid")
        ).thenReturn(false);

        when(playerRepository.save(player)).thenReturn(player);

        Player result = service.resolvePuuid(player);

        assertThat(result).isSameAs(player);
        assertThat(result.getRiotPuuid()).isEqualTo("resolved-puuid");

        verify(accountClient).getAccount(
            "Psilonnix",
            "EUW"
        );

        verify(playerRepository)
            .existsByRiotPuuid("resolved-puuid");

        verify(playerRepository).save(player);
    }

    /**
     * Verifies that a PUUID already assigned to another player is rejected.
     */
    @Test
    void shouldRejectPuuidAlreadyAssignedToAnotherPlayer() {
        Player player = createPlayer();

        HenrikAccount account = new HenrikAccount(
            "duplicate-puuid",
            "Psilonnix",
            "EUW"
        );

        when(
            accountClient.getAccount(
                "Psilonnix",
                "EUW"
            )
        ).thenReturn(account);

        when(
            playerRepository.existsByRiotPuuid("duplicate-puuid")
        ).thenReturn(true);

        assertThatThrownBy(() -> service.resolvePuuid(player))
            .isInstanceOf(PlayerAccountConflictException.class)
            .hasMessage(
                "Riot PUUID is already assigned to another tracked player"
            );

        assertThat(player.getRiotPuuid()).isNull();

        verify(accountClient).getAccount(
            "Psilonnix",
            "EUW"
        );

        verify(playerRepository)
            .existsByRiotPuuid("duplicate-puuid");

        verify(playerRepository, never()).save(player);
    }

    /**
     * Creates a tracked player without a PUUID.
     *
     * @return player used by the tests
     */
    private Player createPlayer() {
        Player player = new Player();
        player.setGameName("Psilonnix");
        player.setTagLine("EUW");
        player.setDisplayName("Psilonnix");

        return player;
    }
}
