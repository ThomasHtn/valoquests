package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.repository.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves local seasons from Henrik match metadata.
 */
@Service
public class SeasonResolutionService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SeasonResolutionService.class);

    /**
     * Repository used to load and persist seasons.
     */
    private final SeasonRepository seasonRepository;

    /**
     * Creates the season resolution service.
     *
     * @param seasonRepository repository holding the seasons discovered so far
     */
    public SeasonResolutionService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    /**
     * Returns the existing season or creates it when first encountered.
     *
     * @param source season metadata returned by Henrik
     * @return persisted local season
     */
    @Transactional
    public Season resolve(HenrikMatchMetadata.HenrikSeason source) {
        if (source == null || source.id() == null || source.id().isBlank()) {
            throw new IllegalArgumentException(
                "match season id must not be blank"
            );
        }

        return seasonRepository.findByExternalId(source.id())
            .map(existing -> updateName(existing, source.shortName()))
            .orElseGet(() -> create(source));
    }

    /**
     * Creates a local season from external metadata.
     */
    private Season create(HenrikMatchMetadata.HenrikSeason source) {
        Season season = new Season();
        season.setExternalId(source.id());
        season.setName(normalizeName(source));
        season.setActive(false);

        Season savedSeason = seasonRepository.save(season);
        LOGGER.info(
            "Created season from Henrik metadata: externalId={} name={}",
            savedSeason.getExternalId(),
            savedSeason.getName()
        );
        return savedSeason;
    }

    /**
     * Updates a season name when Henrik provides a newer usable value.
     */
    private Season updateName(Season season, String name) {
        if (name != null && !name.isBlank() && !name.equals(season.getName())) {
            LOGGER.debug(
                "Updating season name: externalId={} oldName={} newName={}",
                season.getExternalId(),
                season.getName(),
                name
            );
            season.setName(name);
        }
        return season;
    }

    /**
     * Uses the external identifier when Henrik does not provide a short name.
     */
    private String normalizeName(HenrikMatchMetadata.HenrikSeason source) {
        return source.shortName() == null || source.shortName().isBlank()
            ? source.id()
            : source.shortName();
    }
}
