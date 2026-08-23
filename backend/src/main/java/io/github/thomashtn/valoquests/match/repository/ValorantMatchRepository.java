package io.github.thomashtn.valoquests.match.repository;

import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for Valorant match entities.
 */
public interface ValorantMatchRepository extends JpaRepository<ValorantMatch, Long> {
    /**
     * Finds the match carrying one Henrik match identifier.
     *
     * <p>This lookup is what makes the import idempotent: a match already stored for one tracked
     * player is reused when a second tracked player of the same game is imported.
     *
     * @param externalMatchId Henrik match identifier
     * @return the matching match when it is already stored
     */
    Optional<ValorantMatch> findByExternalMatchId(String externalMatchId);
}
