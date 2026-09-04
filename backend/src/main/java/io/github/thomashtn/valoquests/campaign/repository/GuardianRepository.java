package io.github.thomashtn.valoquests.campaign.repository;

import io.github.thomashtn.valoquests.campaign.entity.Guardian;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to the guardian catalogue.
 */
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    /**
     * Returns every drawable entry of one weight class, in a stable order.
     *
     * @param category weight class to draw from
     * @return enabled entries, lowest identifier first
     */
    List<Guardian> findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory category);
}
