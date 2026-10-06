package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.henrik.client.HenrikAccountClient;
import io.github.thomashtn.valoquests.henrik.model.HenrikAccount;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerAccountConflictException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resolves and stores the stable Riot PUUID of a tracked player.
 */
@Service
public class PlayerAccountResolutionService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PlayerAccountResolutionService.class);

    /**
     * External client used to resolve Riot accounts through Henrik.
     */
    private final HenrikAccountClient accountClient;

    /**
     * Repository used to verify and persist tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Clock stamping the update of the resolved player.
     */
    private final Clock clock;

    /**
     * Creates the player account resolution service.
     *
     * @param accountClient Henrik account client
     * @param playerRepository tracked player repository
     * @param clock application clock
     */
    public PlayerAccountResolutionService(
        HenrikAccountClient accountClient,
        PlayerRepository playerRepository,
        Clock clock
    ) {
        this.accountClient = accountClient;
        this.playerRepository = playerRepository;
        this.clock = clock;
    }

    /**
     * Resolves and stores the player's Riot PUUID when it is not already known.
     *
     * <p>No external request is performed when the player already has a PUUID.
     * This makes the operation idempotent and avoids unnecessary Henrik calls.</p>
     *
     * @param player tracked player to resolve
     * @return player containing a Riot PUUID
     * @throws IllegalArgumentException when the player is null
     * @throws PlayerAccountConflictException when the resolved PUUID already
     *                                        belongs to another player
     * @throws IllegalStateException when the player's Riot identity changed during the resolution
     */
    public Player resolvePuuid(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player must not be null");
        }

        if (hasPuuid(player)) {
            LOGGER.debug(
                "Skipping Riot account resolution for player {} because the PUUID is already known",
                player.getId()
            );
            return player;
        }

        LOGGER.info(
            "Resolving Riot account for player {} ({})",
            player.getId(),
            player.getDisplayName()
        );

        HenrikAccount account = accountClient.getAccount(
            player.getGameName(),
            player.getTagLine()
        );

        verifyPuuidAvailability(account.puuid());

        int updatedRows = playerRepository.storeResolvedPuuid(
            player.getId(),
            player.getGameName(),
            player.getTagLine(),
            account.puuid(),
            clock.instant()
        );
        if (updatedRows == 0) {
            // Storing it anyway would import the matches of the account the player no longer names.
            throw new IllegalStateException(
                "Player " + player.getId() + " changed while its Riot account was being resolved"
            );
        }
        player.setRiotPuuid(account.puuid());

        LOGGER.info(
            "Resolved Riot account for player {}",
            player.getId()
        );

        return player;
    }

    /**
     * Determines whether a player already has a usable Riot PUUID.
     *
     * @param player tracked player
     * @return {@code true} when the PUUID is present and non-blank
     */
    private boolean hasPuuid(Player player) {
        return player.getRiotPuuid() != null
            && !player.getRiotPuuid().isBlank();
    }

    /**
     * Ensures that the resolved Riot account is not assigned to another player.
     *
     * @param riotPuuid resolved stable Riot account identifier
     * @throws PlayerAccountConflictException when the PUUID already exists
     */
    private void verifyPuuidAvailability(String riotPuuid) {
        if (riotPuuid == null || riotPuuid.isBlank()) {
            throw new IllegalStateException(
                "Henrik account response does not contain a Riot PUUID"
            );
        }

        if (playerRepository.existsByRiotPuuid(riotPuuid)) {
            throw new PlayerAccountConflictException(
                "Riot PUUID is already assigned to another tracked player"
            );
        }
    }
}
