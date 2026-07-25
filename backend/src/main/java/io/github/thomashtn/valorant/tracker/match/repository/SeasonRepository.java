package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.Season;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for season entities.
 */
public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByExternalId(String externalId);
}
