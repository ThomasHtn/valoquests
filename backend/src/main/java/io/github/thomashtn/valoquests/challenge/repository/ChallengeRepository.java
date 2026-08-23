package io.github.thomashtn.valoquests.challenge.repository;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for challenge catalogue entities.
 */
public interface ChallengeRepository
    extends JpaRepository<Challenge, Long> {

    /**
     * Retrieves every challenge eligible for weekly selection.
     *
     * @return enabled challenges ordered by identifier
     */
    List<Challenge> findAllByEnabledTrueOrderByIdAsc();
}
