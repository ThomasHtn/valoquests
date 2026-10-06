package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.MatchCorrectionResponse;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;

/**
 * Applies administrator-issued corrections to a stored match.
 */
public interface MatchCorrectionService {

    /**
     * Overrides the game mode stored for one match, as {@link GameModeSource#MANUALLY_CORRECTED}.
     *
     * @param matchId  internal match identifier
     * @param gameMode mode to apply
     * @return the match state after the correction
     * @throws MatchNotFoundException when no stored match owns the identifier
     */
    MatchCorrectionResponse correctGameMode(Long matchId, GameMode gameMode);
}
