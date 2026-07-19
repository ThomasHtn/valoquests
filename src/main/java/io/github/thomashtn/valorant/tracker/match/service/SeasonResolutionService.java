package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the local season associated with Henrik match metadata. */
@Service
public class SeasonResolutionService {
    private final SeasonRepository seasonRepository;

    public SeasonResolutionService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public Season resolve(HenrikMatchMetadata.HenrikSeason source) {
        if (source == null || source.id() == null || source.id().isBlank()) {
            throw new IllegalArgumentException("match season id must not be blank");
        }

        return seasonRepository.findByExternalId(source.id())
            .map(existing -> updateName(existing, source.shortName()))
            .orElseGet(() -> create(source));
    }

    private Season create(HenrikMatchMetadata.HenrikSeason source) {
        Season season = new Season();
        season.setExternalId(source.id());
        season.setName(normalizeName(source));
        season.setActive(false);
        return seasonRepository.save(season);
    }

    private Season updateName(Season season, String name) {
        if (name != null && !name.isBlank() && !name.equals(season.getName())) {
            season.setName(name);
        }
        return season;
    }

    private String normalizeName(HenrikMatchMetadata.HenrikSeason source) {
        return source.shortName() == null || source.shortName().isBlank()
            ? source.id()
            : source.shortName();
    }
}
