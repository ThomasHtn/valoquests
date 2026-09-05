package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.ChallengeCatalogueResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the enabled challenge catalogue exposed by the catalogue endpoint.
 */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeCatalogueQueryService implements ChallengeCatalogueQueryService {

    /**
     * Repository used to retrieve the challenge catalogue.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Parser used to read base definitions.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Resolver scaling base targets to the calibration in force.
     */
    private final ChallengeTargetResolver targetResolver;

    /**
     * Barème saying what a challenge of each weight is worth.
     */
    private final ScoringRuleset ruleset;

    /**
     * Source of the calibration in force this week.
     */
    private final ChallengeCalibrationSource calibrationSource;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the challenge-catalogue query service.
     *
     * @param challengeRepository challenge catalogue repository
     * @param definitionParser    challenge-definition parser
     * @param targetResolver      target resolver
     * @param ruleset             scoring ruleset
     * @param calibrationSource   calibration source
     * @param weekCalendar        calendar resolving the current week
     */
    public DefaultChallengeCatalogueQueryService(
        ChallengeRepository challengeRepository,
        ChallengeDefinitionParser definitionParser,
        ChallengeTargetResolver targetResolver,
        ScoringRuleset ruleset,
        ChallengeCalibrationSource calibrationSource,
        WeekCalendar weekCalendar
    ) {
        this.challengeRepository = challengeRepository;
        this.definitionParser = definitionParser;
        this.targetResolver = targetResolver;
        this.ruleset = ruleset;
        this.calibrationSource = calibrationSource;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns every enabled challenge, as it would be drawn this week.
     *
     * <p>Targets are resolved against the calibration in force the same way a draw resolves them,
     * so a catalogue entry and that same challenge once drawn this week agree. A challenge drawn
     * in a past campaign keeps the targets it was drawn with; only the catalogue moves.
     *
     * @return the enabled challenge catalogue, ordered by identifier
     */
    @Override
    public ChallengeCatalogueResponse findCatalogue() {
        ChallengeCalibration calibration = calibrationSource.forWeek(weekCalendar.currentWeekStart());

        List<ChallengeCatalogueResponse.ChallengeCatalogueEntry> entries = challengeRepository
            .findAllByEnabledTrueOrderByIdAsc()
            .stream()
            .map(challenge -> toCatalogueEntry(challenge, calibration))
            .toList();

        return new ChallengeCatalogueResponse(calibration.reference(), entries);
    }

    /**
     * Converts one catalogue challenge into an API response.
     *
     * @param challenge   catalogue challenge to convert
     * @param calibration calibration in force
     * @return catalogue entry
     */
    private ChallengeCatalogueResponse.ChallengeCatalogueEntry toCatalogueEntry(
        Challenge challenge,
        ChallengeCalibration calibration
    ) {
        ChallengeDefinition base = definitionParser.parse(challenge);
        ChallengeDefinition definition = targetResolver.resolve(
            base,
            challenge.getCadence(),
            challenge.getDifficulty(),
            calibration.scaling()
        );
        double weight = ruleset.challengeWeight(challenge.getCadence(), challenge.getDifficulty());

        return new ChallengeCatalogueResponse.ChallengeCatalogueEntry(
            challenge.getId(),
            challenge.getCode(),
            challenge.getName(),
            ChallengeDescriptionResolver.resolve(challenge.getDescription(), base, definition),
            challenge.getCadence(),
            challenge.getDifficulty(),
            definition.isCompetitiveOnly(),
            ChallengeMetricLabels.of(definition),
            definition.progressTarget(),
            ruleset.challengeSurvivors(calibration.reference(), weight, calibration.weekIndex()),
            ruleset.challengeRankingPoints(calibration.reference(), weight, calibration.weekIndex())
        );
    }
}
