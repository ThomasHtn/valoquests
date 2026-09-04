package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Builds selections with their conditions resolved against the calibration in force.
 *
 * <p>The draw resolves, the row stores, everything else reads: this is the one place a base
 * definition becomes the definition a week is actually played against.
 */
@Component
public class ChallengeSelectionFactory {

    /**
     * Parser reading base definitions and writing resolved ones.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Resolver scaling base targets.
     */
    private final ChallengeTargetResolver targetResolver;

    /**
     * Source of the calibration a week is drawn against.
     */
    private final ChallengeCalibrationSource calibrationSource;

    /**
     * Creates the factory.
     *
     * @param definitionParser  challenge-definition parser
     * @param targetResolver    target resolver
     * @param calibrationSource calibration source
     */
    public ChallengeSelectionFactory(
        ChallengeDefinitionParser definitionParser,
        ChallengeTargetResolver targetResolver,
        ChallengeCalibrationSource calibrationSource
    ) {
        this.definitionParser = definitionParser;
        this.targetResolver = targetResolver;
        this.calibrationSource = calibrationSource;
    }

    /**
     * Creates one weekly selection.
     *
     * @param weekStart     Monday identifying the week
     * @param challenge     drawn catalogue challenge
     * @param selectionTime draw timestamp
     * @return unsaved selection carrying its resolved conditions
     */
    public WeeklyChallenge weekly(LocalDate weekStart, Challenge challenge, Instant selectionTime) {
        return create(weekStart, null, challenge, selectionTime);
    }

    /**
     * Creates one daily selection.
     *
     * @param weekStart     Monday of the week the day belongs to
     * @param day           day the selection covers
     * @param challenge     drawn catalogue challenge
     * @param selectionTime draw timestamp
     * @return unsaved selection carrying its resolved conditions
     */
    public WeeklyChallenge daily(
        LocalDate weekStart,
        LocalDate day,
        Challenge challenge,
        Instant selectionTime
    ) {
        return create(weekStart, day, challenge, selectionTime);
    }

    /**
     * Creates one selection of either cadence.
     *
     * @param weekStart     Monday identifying the week
     * @param day           covered day, {@code null} for a weekly selection
     * @param challenge     drawn catalogue challenge
     * @param selectionTime draw timestamp
     * @return unsaved selection
     */
    private WeeklyChallenge create(
        LocalDate weekStart,
        LocalDate day,
        Challenge challenge,
        Instant selectionTime
    ) {
        ChallengeCadence cadence = day == null ? ChallengeCadence.WEEKLY : ChallengeCadence.DAILY;
        ChallengeCalibration calibration = calibrationSource.forWeek(weekStart);
        ChallengeDefinition resolved = targetResolver.resolve(
            definitionParser.parse(challenge),
            cadence,
            challenge.getDifficulty(),
            calibration.scaling()
        );

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setWeekStart(weekStart);
        selection.setCadence(cadence);
        selection.setDay(day);
        selection.setChallenge(challenge);
        selection.setSelectedAt(selectionTime);
        selection.setResolvedConditionsJson(definitionParser.toJson(resolved.conditions()));

        return selection;
    }
}
