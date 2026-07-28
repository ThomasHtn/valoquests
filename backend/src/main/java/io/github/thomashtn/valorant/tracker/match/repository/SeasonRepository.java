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
     * <p>Ordered by identifier rather than {@code startsAt}/{@code endsAt}: seasons are created
     * on demand from Henrik match metadata and those two fields are never populated, so insertion
     * order is the only reliable signal of chronological recency.</p>
     */
    List<Season> findAllByOrderByIdDesc();
}
