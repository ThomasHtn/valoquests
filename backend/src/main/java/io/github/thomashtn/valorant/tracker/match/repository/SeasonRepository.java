package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.Season;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for season entities.
 */
public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByExternalId(String externalId);

    /**
     * Returns every season, most recently discovered first.
     *
     * <p>Ordered by identifier rather than {@code startsAt}/{@code endsAt}, which are never
     * populated: seasons are created on demand from Henrik match metadata. Insertion order is not
     * chronological either, so this only provides a deterministic order for callers to sort;
     * {@code DefaultSeasonQueryService} reorders by episode and act before exposing them.</p>
     */
    List<Season> findAllByOrderByIdDesc();
}
