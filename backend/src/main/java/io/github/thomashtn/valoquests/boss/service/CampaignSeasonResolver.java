package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.repository.SeasonRepository;
import io.github.thomashtn.valoquests.match.service.SeasonQueryService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the Valorant act the campaign currently runs in.
 *
 * <p>The campaign is not the whole boss history: it is the run of fights belonging to the act in
 * progress, so a new act starts a fresh map rather than extending a timeline that would grow without
 * end. This is the single place that answers "which act is that", so the service stamping a new
 * fight and the service listing the campaign cannot disagree on it — a disagreement would either
 * hide the running week from its own campaign or keep a closed act on the map.
 *
 * <p>Acts are discovered from imported matches and carry no dates (see {@code SeasonResolutionService}),
 * so the act in progress is the chronologically latest one known, exactly as the player profile
 * already resolves the season its history defaults to.
 */
@Service
public class CampaignSeasonResolver {

    /**
     * Service ordering known seasons and naming the most recent one.
     */
    private final SeasonQueryService seasonQueryService;

    /**
     * Repository used to load the resolved act as an entity, to stamp it on a fight.
     */
    private final SeasonRepository seasonRepository;

    /**
     * Creates the campaign season resolver.
     *
     * @param seasonQueryService season query service
     * @param seasonRepository   season repository
     */
    public CampaignSeasonResolver(
        SeasonQueryService seasonQueryService,
        SeasonRepository seasonRepository
    ) {
        this.seasonQueryService = seasonQueryService;
        this.seasonRepository = seasonRepository;
    }

    /**
     * Returns the identifier of the act the campaign currently runs in.
     *
     * @return current act identifier, or empty while no match has been imported yet
     */
    @Transactional(readOnly = true)
    public Optional<Long> currentSeasonId() {
        return Optional.ofNullable(seasonQueryService.resolveCurrentSeasonId());
    }

    /**
     * Returns the act the campaign currently runs in.
     *
     * @return current act, or empty while no match has been imported yet
     */
    @Transactional(readOnly = true)
    public Optional<Season> currentSeason() {
        return currentSeasonId().flatMap(seasonRepository::findById);
    }
}
