package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.henrik.client.HenrikAccountClient;
import io.github.thomashtn.valoquests.henrik.model.HenrikAccount;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerAccountConflictException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
     * Instant every update is stamped with.
     */
    private static final Instant NOW = Instant.parse("2026-07-20T10:00:00Z");

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
            playerRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
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

        when(playerRepository.storeResolvedPuuid(1L, "Psilonnix", "EUW", "resolved-puuid", NOW))
            .thenReturn(1);

        Player result = service.resolvePuuid(player);

        assertThat(result).isSameAs(player);
        assertThat(result.getRiotPuuid()).isEqualTo("resolved-puuid");

        verify(accountClient).getAccount(
            "Psilonnix",
            "EUW"
        );

        verify(playerRepository)
            .existsByRiotPuuid("resolved-puuid");

        verify(playerRepository)
            .storeResolvedPuuid(1L, "Psilonnix", "EUW", "resolved-puuid", NOW);
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

        when(playerRepository.storeResolvedPuuid(1L, "Psilonnix", "EUW", "resolved-puuid", NOW))
            .thenReturn(1);

        Player result = service.resolvePuuid(player);

        assertThat(result).isSameAs(player);
        assertThat(result.getRiotPuuid()).isEqualTo("resolved-puuid");

        verify(accountClient).getAccount(
            "Psilonnix",
            "EUW"
        );

        verify(playerRepository)
            .existsByRiotPuuid("resolved-puuid");

        verify(playerRepository)
            .storeResolvedPuuid(1L, "Psilonnix", "EUW", "resolved-puuid", NOW);
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
            .isInstanceOf(ConflictException.class)
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

        verify(playerRepository, never())
            .storeResolvedPuuid(any(), anyString(), anyString(), anyString(), any());
    }

    /**
     * Verifies that a PUUID resolved for a Riot identity edited meanwhile is not stored.
     */
    @Test
    @DisplayName("Fails without keeping the PUUID when the Riot identity changed during the lookup")
    void shouldFailWhenTheIdentityChangedDuringTheLookup() {
        Player player = createPlayer();

        when(accountClient.getAccount("Psilonnix", "EUW"))
            .thenReturn(new HenrikAccount("resolved-puuid", "Psilonnix", "EUW"));
        when(playerRepository.existsByRiotPuuid("resolved-puuid")).thenReturn(false);
        when(playerRepository.storeResolvedPuuid(1L, "Psilonnix", "EUW", "resolved-puuid", NOW))
            .thenReturn(0);

        assertThatThrownBy(() -> service.resolvePuuid(player))
            .isInstanceOf(IllegalStateException.class);

        assertThat(player.getRiotPuuid()).isNull();
    }

    /**
     * Creates a tracked player without a PUUID.
     *
     * @return player used by the tests
     */
    private Player createPlayer() {
        Player player = new Player();
        player.setId(1L);
        player.setGameName("Psilonnix");
        player.setTagLine("EUW");
        player.setDisplayName("Psilonnix");

        return player;
    }
}
