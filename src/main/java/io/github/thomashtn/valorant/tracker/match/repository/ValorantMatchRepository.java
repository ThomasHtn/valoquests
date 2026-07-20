package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for Valorant match entities.
 */
public interface ValorantMatchRepository extends JpaRepository<ValorantMatch, Long> {
    Optional<ValorantMatch> findByExternalMatchId(String externalMatchId);
}
