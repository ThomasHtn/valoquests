package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.exception.MatchNotFoundException;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.GameModeSource;
import io.github.thomashtn.valorant.tracker.match.repository.ValorantMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies administrator-issued corrections to a stored match.
 *
 * <p>A correction is recorded as {@link GameModeSource#MANUALLY_CORRECTED}, the highest-priority
 * source: {@link MatchImportService} never overwrites it, however confidently a later synchronization
 * resolves the mode Henrik reports for the same match.
 */
@Service
public class MatchCorrectionService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(MatchCorrectionService.class);

    /**
     * Repository used to load and persist Valorant matches.
     */
    private final ValorantMatchRepository matchRepository;

    /**
     * Creates the match correction service.
     *
     * @param matchRepository repository holding Valorant matches
     */
    public MatchCorrectionService(ValorantMatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Overrides the game mode stored for one match.
     *
     * @param matchId  internal match identifier
     * @param gameMode mode to apply
     * @return the corrected match
     */
    @Transactional
    public ValorantMatch correctGameMode(Long matchId, GameMode gameMode) {
        ValorantMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));

        LOGGER.info(
            "Manually correcting game mode for match {}: {} ({}) -> {} (MANUALLY_CORRECTED)",
            matchId,
            match.getGameMode(),
            match.getGameModeSource(),
            gameMode
        );

        match.setGameMode(gameMode);
        match.setGameModeSource(GameModeSource.MANUALLY_CORRECTED);
        return matchRepository.save(match);
    }
}
