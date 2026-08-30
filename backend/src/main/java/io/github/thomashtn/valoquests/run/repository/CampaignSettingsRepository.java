package io.github.thomashtn.valoquests.run.repository;

import io.github.thomashtn.valoquests.run.entity.CampaignSettings;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for the single {@link CampaignSettings} row.
 */
public interface CampaignSettingsRepository extends JpaRepository<CampaignSettings, Short> {
}
