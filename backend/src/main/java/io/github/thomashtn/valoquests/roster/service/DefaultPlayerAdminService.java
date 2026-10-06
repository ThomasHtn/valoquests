package io.github.thomashtn.valoquests.roster.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.service.CampaignRosterMembership;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.roster.dto.PlayerAdminResponse;
import io.github.thomashtn.valoquests.roster.dto.PlayerCreateRequest;
import io.github.thomashtn.valoquests.roster.dto.PlayerDeletionResponse;
import io.github.thomashtn.valoquests.roster.dto.PlayerUpdateRequest;
import io.github.thomashtn.valoquests.roster.model.PlayerDeletionOutcome;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the tracked roster, guarding the frozen roster of the live campaign.
 */
@Service
public class DefaultPlayerAdminService implements PlayerAdminService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPlayerAdminService.class);

    /**
     * Days without a match past which an active player is flagged, as they still raise the squad's targets.
     */
    private static final int IDLE_THRESHOLD_DAYS = 14;

    /**
     * Repository owning the tracked roster.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository telling which players played recently.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Service deleting a player and every row referencing it.
     */
    private final PlayerRemovalService playerRemovalService;

    /**
     * Resolver deciding whether a player may be deleted outright.
     */
    private final CampaignRosterMembership rosterMembership;

    /**
     * Application clock, deciding how far back "recently" reaches.
     */
    private final Clock clock;

    /**
     * Creates the player administration service.
     *
     * @param playerRepository      tracked player repository
     * @param playerMatchRepository player match repository
     * @param playerRemovalService  service deleting a player for good
     * @param rosterMembership      campaign roster membership
     * @param clock                 application clock, deciding how far back "recently" is
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultPlayerAdminService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        PlayerRemovalService playerRemovalService,
        CampaignRosterMembership rosterMembership,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.playerRemovalService = playerRemovalService;
        this.rosterMembership = rosterMembership;
        this.clock = clock;
    }

    /**
     * Returns every player, archived ones included.
     *
     * <p>Unlike the public listing, archived players stay visible so they can be restored.
     *
     * @return every tracked player, ordered by identifier
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlayerAdminResponse> findAll() {
        return toResponses(playerRepository.findAllByOrderByIdAsc());
    }

    /**
     * Adds a player to the tracked roster.
     *
     * <p>The PUUID is left to the first synchronization, so adding a player never depends on Henrik.
     *
     * @param request player identity
     * @return the created player
     * @throws ConflictException when the Riot identity is already tracked, or an active player is
     *                           added while a campaign is live
     */
    @Override
    @Transactional
    public PlayerAdminResponse create(PlayerCreateRequest request) {
        rejectDuplicateRiotIdentity(request.gameName(), request.tagLine(), null);
        rejectActivationDuringCampaign(request.status());

        Player player = new Player();

        player.setGameName(request.gameName());
        player.setTagLine(request.tagLine());
        player.setDisplayName(request.displayName());
        player.setPortrait(request.portrait());
        player.setStatus(request.status());

        Player saved = playerRepository.save(player);

        LOGGER.info("Player {} added to the tracked roster", saved.getId());

        return toResponse(saved);
    }

    /**
     * Updates the identity of a tracked player.
     *
     * <p>Changing the Riot identity clears the stored PUUID, or the previous account's matches would
     * keep being imported.
     *
     * @param playerId tracked player identifier
     * @param request  new identity
     * @return the updated player
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     * @throws ConflictException       when the Riot identity belongs to another player
     */
    @Override
    @Transactional
    public PlayerAdminResponse update(long playerId, PlayerUpdateRequest request) {
        Player player = requirePlayer(playerId);

        rejectDuplicateRiotIdentity(request.gameName(), request.tagLine(), player);

        if (hasRiotIdentityChanged(player, request)) {
            player.setRiotPuuid(null);
        }

        player.setGameName(request.gameName());
        player.setTagLine(request.tagLine());
        player.setDisplayName(request.displayName());
        player.setPortrait(request.portrait());

        return toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public PlayerAdminResponse changeStatus(long playerId, PlayerStatus status) {
        Player player = requirePlayer(playerId);

        if (player.getStatus() != status) {
            rejectLiveRosterChange(playerId);
            rejectActivationDuringCampaign(status);
        }
        player.setStatus(status);

        LOGGER.info("Player {} moved to status {}", playerId, status);

        return toResponse(playerRepository.save(player));
    }

    /**
     * Removes a player from the roster, by deletion or by archiving.
     *
     * <p>A player who was on any campaign roster is archived, since finalized weeks are immutable; any
     * other is deleted with all its rows.
     *
     * @param playerId tracked player identifier
     * @return what the request actually did
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     * @throws ConflictException       when the live campaign's roster counts the player
     */
    @Override
    @Transactional
    public PlayerDeletionResponse removeFromRoster(long playerId) {
        Player player = requirePlayer(playerId);
        rejectLiveRosterChange(playerId);

        if (rosterMembership.wasOnAnyRoster(playerId)) {
            player.setStatus(PlayerStatus.ARCHIVED);
            playerRepository.save(player);

            LOGGER.info(
                "Player {} archived instead of deleted: finalized campaign data depends on it",
                playerId
            );

            return new PlayerDeletionResponse(playerId, PlayerDeletionOutcome.ARCHIVED);
        }

        playerRemovalService.delete(player);

        LOGGER.info("Player {} deleted from the tracked roster", playerId);

        return new PlayerDeletionResponse(playerId, PlayerDeletionOutcome.DELETED);
    }

    /**
     * Loads a tracked player or fails.
     *
     * @param playerId tracked player identifier
     * @return the tracked player
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     */
    private Player requirePlayer(long playerId) {
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
    }

    /**
     * Refuses to touch a player the live campaign's frozen roster counts.
     *
     * <p>Applied at once, the change would stop importing their matches or drop them from the
     * ranking while the campaign's guardians stay sized on them.
     *
     * @param playerId tracked player identifier
     * @throws ConflictException when the live campaign counts the player
     */
    private void rejectLiveRosterChange(long playerId) {
        if (rosterMembership.isOnLiveRoster(playerId)) {
            throw new ConflictException(
                "Player " + playerId + " is on the roster of the campaign in progress: "
                    + "their status is frozen until it ends."
            );
        }
    }

    /**
     * Refuses to make a player active while a campaign runs on a roster they are not on.
     *
     * @param status status requested
     * @throws ConflictException when a campaign is live and the status is active
     */
    private void rejectActivationDuringCampaign(PlayerStatus status) {
        if (status == PlayerStatus.ACTIVE && rosterMembership.isCampaignLive()) {
            throw new ConflictException(
                "A campaign is in progress: a player only becomes active when the next one opens."
            );
        }
    }

    /**
     * Refuses a Riot identity another tracked player already holds.
     *
     * @param gameName Riot game name
     * @param tagLine  Riot tag line
     * @param current  player being edited, or {@code null} when creating one
     * @throws ConflictException when the identity is already taken
     */
    private void rejectDuplicateRiotIdentity(String gameName, String tagLine, Player current) {
        boolean unchanged = current != null
            && current.getGameName().equalsIgnoreCase(gameName)
            && current.getTagLine().equalsIgnoreCase(tagLine);

        if (unchanged) {
            return;
        }

        if (playerRepository.existsByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)) {
            throw new ConflictException(
                "Riot identity " + gameName + "#" + tagLine + " is already tracked."
            );
        }
    }

    /**
     * Determines whether an update designates a different Riot account.
     *
     * @param player  player being edited
     * @param request new identity
     * @return {@code true} when the Riot identity changed
     */
    private boolean hasRiotIdentityChanged(Player player, PlayerUpdateRequest request) {
        return !player.getGameName().equalsIgnoreCase(request.gameName())
            || !player.getTagLine().equalsIgnoreCase(request.tagLine());
    }

    /**
     * Converts a tracked player into its administration representation.
     *
     * @param player tracked player
     * @return administration representation
     */
    private PlayerAdminResponse toResponse(Player player) {
        return toResponses(List.of(player)).getFirst();
    }

    /**
     * Converts tracked players into their administration representation in two queries.
     *
     * @param players tracked players
     * @return administration representations, in the same order
     */
    private List<PlayerAdminResponse> toResponses(List<Player> players) {
        Set<Long> rosteredPlayerIds = rosterMembership.rosteredPlayerIds();
        Set<Long> recentlyActiveIds = playerMatchRepository.findPlayerIdsWithMatchStartedSince(
            clock.instant().minus(Duration.ofDays(IDLE_THRESHOLD_DAYS))
        );

        return players.stream()
            .map(player -> new PlayerAdminResponse(
                player.getId(),
                player.getGameName(),
                player.getTagLine(),
                player.getDisplayName(),
                player.getPortrait(),
                player.getStatus(),
                player.getRiotPuuid(),
                player.getLastSuccessfulSynchronizationAt(),
                rosteredPlayerIds.contains(player.getId()),
                recentlyActiveIds.contains(player.getId())
            ))
            .toList();
    }
}
