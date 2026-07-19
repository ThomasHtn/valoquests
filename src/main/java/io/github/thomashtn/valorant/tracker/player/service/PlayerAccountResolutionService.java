package io.github.thomashtn.valorant.tracker.player.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikAccountClient;
import io.github.thomashtn.valorant.tracker.henrik.model.HenrikAccount;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerAccountConflictException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;

/**
 * Resolves and stores the stable Riot PUUID of a tracked player.
 */
@Service
public class PlayerAccountResolutionService {

    /**
     * External client used to resolve Riot accounts through Henrik.
     */
    private final HenrikAccountClient accountClient;

    /**
     * Repository used to verify and persist tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Creates the player account resolution service.
     *
     * @param accountClient Henrik account client
     * @param playerRepository tracked player repository
     */
    public PlayerAccountResolutionService(
        HenrikAccountClient accountClient,
        PlayerRepository playerRepository
    ) {
        this.accountClient = accountClient;
        this.playerRepository = playerRepository;
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
     */
    public Player resolvePuuid(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player must not be null");
        }

        if (hasPuuid(player)) {
            return player;
        }

        HenrikAccount account = accountClient.getAccount(
            player.getGameName(),
            player.getTagLine()
        );

        verifyPuuidAvailability(account.puuid());

        player.setRiotPuuid(account.puuid());

        return playerRepository.save(player);
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
        if (playerRepository.existsByRiotPuuid(riotPuuid)) {
            throw new PlayerAccountConflictException(
                "Riot PUUID is already assigned to another tracked player"
            );
        }
    }
}
