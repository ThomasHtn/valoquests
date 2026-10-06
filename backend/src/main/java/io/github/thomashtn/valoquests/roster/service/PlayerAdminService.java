package io.github.thomashtn.valoquests.roster.service;

import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.roster.dto.PlayerAdminResponse;
import io.github.thomashtn.valoquests.roster.dto.PlayerCreateRequest;
import io.github.thomashtn.valoquests.roster.dto.PlayerDeletionResponse;
import io.github.thomashtn.valoquests.roster.dto.PlayerUpdateRequest;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import java.util.List;

/**
 * Manages the tracked roster on behalf of the administration screens.
 */
public interface PlayerAdminService {

    /**
     * Returns every player, archived ones included.
     *
     * @return every tracked player, ordered by identifier
     */
    List<PlayerAdminResponse> findAll();

    /**
     * Adds a player to the tracked roster.
     *
     * @param request player identity
     * @return the created player
     * @throws ConflictException when the Riot identity is already tracked, or an active player is
     *                           added while a campaign is live
     */
    PlayerAdminResponse create(PlayerCreateRequest request);

    /**
     * Updates the identity of a tracked player.
     *
     * @param playerId tracked player identifier
     * @param request  new identity
     * @return the updated player
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     * @throws ConflictException       when the Riot identity belongs to another player
     */
    PlayerAdminResponse update(long playerId, PlayerUpdateRequest request);

    /**
     * Moves a tracked player to another lifecycle status.
     *
     * @param playerId tracked player identifier
     * @param status   status to apply
     * @return the updated player
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     * @throws ConflictException       when the live campaign's roster counts the player, or the
     *                                 player would become active while a campaign is live
     */
    PlayerAdminResponse changeStatus(long playerId, PlayerStatus status);

    /**
     * Removes a player from the roster, by deletion or by archiving.
     *
     * @param playerId tracked player identifier
     * @return what the request actually did
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     * @throws ConflictException       when the live campaign's roster counts the player
     */
    PlayerDeletionResponse removeFromRoster(long playerId);
}
