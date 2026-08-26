package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.dto.PlayerAdminResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerCreateRequest;
import io.github.thomashtn.valoquests.player.dto.PlayerDeletionResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerUpdateRequest;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerDeletionOutcome;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.synchronization.repository.PlayerSeasonSynchronizationRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the tracked roster on behalf of the administration screens.
 */
@Service
public class PlayerAdminService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerAdminService.class);

    /**
     * Days without a single match past which an active player is flagged as forgotten.
     *
     * <p>Two weeks, which is about as long as the colony's own memory: the fifteen-percent nightly
     * catch-up means an inhabitant gained a fortnight ago has almost entirely gone, so a player
     * absent that long has stopped contributing anything the score still carries.
     *
     * <p>Worth flagging because it costs real points. The roster size drives the turnout denominator,
     * the opening housing, the fight's materials and the fight's hit points all at once, so a player
     * left active and away widens the town without feeding it — and since the lower of the two
     * ceilings commands, the score follows the food. Roughly five percent of a run's final score for
     * one forgotten account. Nobody would guess that from the screen, which is why it is written on
     * it.
     */
    private static final int IDLE_THRESHOLD_DAYS = 14;

    /**
     * Repository owning the tracked roster.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to drop the match history of a player being deleted.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Repository used to drop the synchronization results of a player being deleted.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Repository used to drop the season checkpoints of a player being deleted.
     */
    private final PlayerSeasonSynchronizationRepository seasonSynchronizationRepository;

    /**
     * Resolver deciding whether a player may be deleted outright.
     */
    private final PlayerCampaignContributionResolver contributionResolver;

    /**
     * Application clock, deciding how far back "recently" reaches.
     */
    private final Clock clock;

    /**
     * Creates the player administration service.
     *
     * @param playerRepository                tracked player repository
     * @param playerMatchRepository           player match repository
     * @param playerResultRepository          synchronization player result repository
     * @param seasonSynchronizationRepository player season synchronization repository
     * @param contributionResolver            campaign contribution resolver
     * @param clock                           application clock, deciding how far back "recently" is
     */
    public PlayerAdminService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        PlayerSeasonSynchronizationRepository seasonSynchronizationRepository,
        PlayerCampaignContributionResolver contributionResolver,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.playerResultRepository = playerResultRepository;
        this.seasonSynchronizationRepository = seasonSynchronizationRepository;
        this.contributionResolver = contributionResolver;
        this.clock = clock;
    }

    /**
     * Returns every player, archived ones included.
     *
     * <p>Unlike the public listing, archiving must stay visible here: it is the administration
     * screen that has to offer restoring one.
     *
     * @return every tracked player, ordered by identifier
     */
    @Transactional(readOnly = true)
    public List<PlayerAdminResponse> findAll() {
        return playerRepository.findAllByOrderByIdAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Adds a player to the tracked roster.
     *
     * <p>The Riot PUUID is left unresolved on purpose. It is resolved by the first synchronization
     * of the player, so calling Henrik here would make adding a player fail whenever the upstream
     * API is momentarily unavailable, for an identifier nothing needs yet.
     *
     * @param request player identity
     * @return the created player
     * @throws ConflictException when the Riot identity is already tracked
     */
    @Transactional
    public PlayerAdminResponse create(PlayerCreateRequest request) {
        rejectDuplicateRiotIdentity(request.gameName(), request.tagLine(), null);

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
     * <p>Changing the Riot identity clears the stored PUUID, so the next synchronization resolves
     * the account the new identity designates. Keeping it would silently go on importing the
     * previous account's matches under the new name.
     *
     * @param playerId tracked player identifier
     * @param request  new identity
     * @return the updated player
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     * @throws ConflictException       when the Riot identity belongs to another player
     */
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

    /**
     * Moves a tracked player to another lifecycle status.
     *
     * @param playerId tracked player identifier
     * @param status   status to apply
     * @return the updated player
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     */
    @Transactional
    public PlayerAdminResponse changeStatus(long playerId, PlayerStatus status) {
        Player player = requirePlayer(playerId);

        player.setStatus(status);

        LOGGER.info("Player {} moved to status {}", playerId, status);

        return toResponse(playerRepository.save(player));
    }

    /**
     * Removes a player from the roster, by deletion or by archiving.
     *
     * <p>A player that fought a boss is archived rather than deleted: finalized weeks may credit it
     * with a kill or hold its ranking position, and those weeks are immutable. A player that never
     * did is removed for good, along with the synchronization traces it left, which carry no
     * historical value of their own.
     *
     * @param playerId tracked player identifier
     * @return what the request actually did
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     */
    @Transactional
    public PlayerDeletionResponse removeFromRoster(long playerId) {
        Player player = requirePlayer(playerId);

        if (contributionResolver.hasContributed(playerId)) {
            player.setStatus(PlayerStatus.ARCHIVED);
            playerRepository.save(player);

            LOGGER.info(
                "Player {} archived instead of deleted: finalized campaign data depends on it",
                playerId
            );

            return new PlayerDeletionResponse(playerId, PlayerDeletionOutcome.ARCHIVED);
        }

        // Loaded then deleted rather than removed with a bulk statement: a player reaching this
        // branch has no campaign contribution, so these rows are few, and going through the
        // persistence context keeps it from handing back entities the database no longer holds.
        playerMatchRepository.deleteAll(
            playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId)
        );
        playerResultRepository.deleteAll(playerResultRepository.findAllByPlayerId(playerId));
        seasonSynchronizationRepository.deleteAll(
            seasonSynchronizationRepository.findAllByPlayerId(playerId)
        );
        playerRepository.delete(player);

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
        return new PlayerAdminResponse(
            player.getId(),
            player.getGameName(),
            player.getTagLine(),
            player.getDisplayName(),
            player.getPortrait(),
            player.getStatus(),
            player.getRiotPuuid(),
            player.getLastSuccessfulSynchronizationAt(),
            contributionResolver.hasContributed(player.getId()),
            hasRecentMatch(player)
        );
    }

    /**
     * Returns whether a player has played at all in the recent past.
     *
     * @param player tracked player
     * @return {@code true} when they have played inside the idle window
     */
    private boolean hasRecentMatch(Player player) {
        return playerMatchRepository.existsByPlayerIdAndMatchStartedAtGreaterThanEqual(
            player.getId(),
            clock.instant().minus(Duration.ofDays(IDLE_THRESHOLD_DAYS))
        );
    }
}
