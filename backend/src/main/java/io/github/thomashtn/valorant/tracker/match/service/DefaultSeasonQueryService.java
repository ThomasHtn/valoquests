package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.match.dto.SeasonResponse;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.repository.SeasonRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements season consultation from persisted season data.
 */
@Service
@Transactional(readOnly = true)
public class DefaultSeasonQueryService implements SeasonQueryService {

    /**
     * Repository used to load persisted seasons.
     */
    private final SeasonRepository seasonRepository;

    /**
     * Creates the persisted season query service.
     *
     * @param seasonRepository repository used to load persisted seasons
     */
    public DefaultSeasonQueryService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    /**
     * Returns every known season, most recently discovered first.
     *
     * @return known seasons
     */
    @Override
    public List<SeasonResponse> findAll() {
        return seasonRepository.findAllByOrderByIdDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    private SeasonResponse toResponse(Season season) {
        return new SeasonResponse(season.getId(), season.getName(), season.isActive());
    }
}
