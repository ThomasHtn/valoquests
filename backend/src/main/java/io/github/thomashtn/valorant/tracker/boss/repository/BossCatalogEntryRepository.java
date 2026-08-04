package io.github.thomashtn.valorant.tracker.boss.repository;

import io.github.thomashtn.valorant.tracker.boss.entity.BossCatalogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for boss catalogue entities.
 */
public interface BossCatalogEntryRepository
    extends JpaRepository<BossCatalogEntry, Long> {

    /**
     * Retrieves every boss eligible for weekly selection.
     *
     * @return enabled catalogue entries ordered by identifier
     */
    List<BossCatalogEntry> findAllByEnabledTrueOrderByIdAsc();
}
