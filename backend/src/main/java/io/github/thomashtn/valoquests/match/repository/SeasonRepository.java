package io.github.thomashtn.valoquests.match.repository;

import io.github.thomashtn.valoquests.match.entity.Season;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for season entities.
 */
public interface SeasonRepository extends JpaRepository<Season, Long> {
    /**
     * Finds the season carrying one Henrik season identifier.
     *
     * @param externalId Henrik season identifier, such as {@code e11a4}
     * @return the matching season when it has already been discovered
     */
    Optional<Season> findByExternalId(String externalId);

    /**
     * Returns every season, most recently discovered first.
     *
     * <p>Ordered by identifier rather than {@code startsAt}/{@code endsAt}, which are never
     * populated: seasons are created on demand from Henrik match metadata. Insertion order is not
     * chronological either, so this only provides a deterministic order for callers to sort;
     * {@code DefaultSeasonQueryService} reorders by episode and act before exposing them.</p>
     *
     * @return every stored season, highest identifier first
     */
    List<Season> findAllByOrderByIdDesc();
}
