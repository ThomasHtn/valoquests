package io.github.thomashtn.valoquests.roster.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.synchronization.repository.PlayerSeasonSynchronizationRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a player for good, together with every row that references it.
 *
 * <p>Only meant for a player no campaign froze into its roster: campaign rows reference roster
 * members only, so the rows deleted here are all that point at such a player. A new table
 * referencing {@code player} must be purged here too, or the deletion fails on its foreign key.
 */
@Service
public class PlayerRemovalService {

    /**
     * Repository owning the tracked roster.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository holding the player's match history.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Repository holding the player's weekly challenge progress.
     */
    private final PlayerChallengeProgressRepository challengeProgressRepository;

    /**
     * Repository holding the player's weekly ranking scores.
     */
    private final WeeklyPlayerScoreRepository weeklyScoreRepository;

    /**
     * Repository holding the player's synchronization results.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Repository holding the player's season walk checkpoints.
     */
    private final PlayerSeasonSynchronizationRepository seasonSynchronizationRepository;

    /**
     * Creates the player removal service.
     *
     * @param playerRepository                tracked player repository
     * @param playerMatchRepository           player match repository
     * @param challengeProgressRepository     player challenge progress repository
     * @param weeklyScoreRepository           weekly player score repository
     * @param playerResultRepository          synchronization player result repository
     * @param seasonSynchronizationRepository player season synchronization repository
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public PlayerRemovalService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        PlayerChallengeProgressRepository challengeProgressRepository,
        WeeklyPlayerScoreRepository weeklyScoreRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        PlayerSeasonSynchronizationRepository seasonSynchronizationRepository
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.challengeProgressRepository = challengeProgressRepository;
        this.weeklyScoreRepository = weeklyScoreRepository;
        this.playerResultRepository = playerResultRepository;
        this.seasonSynchronizationRepository = seasonSynchronizationRepository;
    }

    /**
     * Deletes a player that never sat on a campaign roster, and every row referencing it.
     *
     * <p>Rows are loaded then deleted rather than removed with bulk statements: such a player has
     * few of them, and going through the persistence context keeps it from handing back entities
     * the database no longer holds.
     *
     * @param player player to delete
     */
    @Transactional
    public void delete(Player player) {
        Long playerId = player.getId();

        challengeProgressRepository.deleteAll(challengeProgressRepository.findAllByPlayerId(playerId));
        weeklyScoreRepository.deleteAll(weeklyScoreRepository.findAllByPlayerId(playerId));
        playerMatchRepository.deleteAll(
            playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId)
        );
        playerResultRepository.deleteAll(playerResultRepository.findAllByPlayerId(playerId));
        seasonSynchronizationRepository.deleteAll(
            seasonSynchronizationRepository.findAllByPlayerId(playerId)
        );
        playerRepository.delete(player);
    }
}
