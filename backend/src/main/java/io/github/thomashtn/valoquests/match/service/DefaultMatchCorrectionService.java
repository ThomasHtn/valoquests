package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.MatchCorrectionResponse;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists administrator-issued corrections to a stored match.
 *
 * <p>A correction is recorded as {@link GameModeSource#MANUALLY_CORRECTED}, the highest-priority
 * source: {@link MatchImportService} never overwrites it, however confidently a later synchronization
 * resolves the mode Henrik reports for the same match.
 */
@Service
public class DefaultMatchCorrectionService implements MatchCorrectionService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultMatchCorrectionService.class);

    /**
     * Repository used to load and persist Valorant matches.
     */
    private final ValorantMatchRepository matchRepository;

    /**
     * Creates the match correction service.
     *
     * @param matchRepository repository holding Valorant matches
     */
    public DefaultMatchCorrectionService(ValorantMatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Override
    @Transactional
    public MatchCorrectionResponse correctGameMode(Long matchId, GameMode gameMode) {
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
        ValorantMatch corrected = matchRepository.save(match);
        return new MatchCorrectionResponse(
            corrected.getId(),
            corrected.getGameMode(),
            corrected.getGameModeSource()
        );
    }
}
