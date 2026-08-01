package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.repository.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Resolves local seasons from Henrik match metadata.
 *
 * <p><strong>Concurrency.</strong> Two different tracked players can both encounter the same new
 * season for the first time in concurrent synchronizations. {@code external_id} is a database-enforced
 * unique constraint, so the loser of that race fails with a constraint violation. Creation runs in its
 * own transaction so that failure can be caught and resolved by reusing the winner's row, regardless
 * of whether the caller already holds an open transaction.
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
     * Runs one creation attempt in its own transaction, independent from any transaction the caller
     * may already be running in.
     */
    private final TransactionTemplate newRowTransactionTemplate;

    /**
     * Creates the season resolution service.
     *
     * @param seasonRepository   repository holding the seasons discovered so far
     * @param transactionManager transaction manager used to isolate racy row creation
     */
    public SeasonResolutionService(
        SeasonRepository seasonRepository,
        PlatformTransactionManager transactionManager
    ) {
        this.seasonRepository = seasonRepository;

        this.newRowTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newRowTransactionTemplate.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
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
            .orElseGet(() -> createOrReuse(source));
    }

    /**
     * Creates a local season from external metadata, reusing the row a concurrent resolution already
     * committed when this call loses the race.
     */
    private Season createOrReuse(HenrikMatchMetadata.HenrikSeason source) {
        try {
            return newRowTransactionTemplate.execute(status -> create(source));
        } catch (DataIntegrityViolationException raceLost) {
            LOGGER.debug(
                "Season {} was created concurrently by another synchronization: reusing it",
                source.id()
            );
            return seasonRepository.findByExternalId(source.id())
                .orElseThrow(() -> raceLost);
        }
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
