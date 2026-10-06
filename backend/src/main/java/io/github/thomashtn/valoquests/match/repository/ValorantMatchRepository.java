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
     * <p>Keeps the import idempotent: a match shared by two tracked players is stored once.
     *
     * @param externalMatchId Henrik match identifier
     * @return the matching match when it is already stored
     */
    Optional<ValorantMatch> findByExternalMatchId(String externalMatchId);
}
