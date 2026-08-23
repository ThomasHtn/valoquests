package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;

/**
 * Defines read operations for weekly challenge data.
 */
public interface ChallengeQueryService {

    /**
     * Returns the active weekly challenges and their collective completion state.
     *
     * @return current weekly challenge data
     */
    CurrentChallengesResponse findCurrent();
}
