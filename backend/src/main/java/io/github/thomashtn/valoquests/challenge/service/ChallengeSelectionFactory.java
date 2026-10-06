package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Builds selections carrying the rule grid the campaign's level plays against.
 *
 * <p>The grid is stored, not chosen again on read, so a replay never rewrites past weeks' objectives.
 */
@Component
public class ChallengeSelectionFactory {

    /**
     * Parser reading base definitions and writing resolved ones.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Source of the calibration a week is drawn against.
     */
    private final ChallengeCalibrationSource calibrationSource;

    /**
     * Creates the factory.
     *
     * @param definitionParser  challenge-definition parser
     * @param calibrationSource calibration source
     */
    public ChallengeSelectionFactory(
        ChallengeDefinitionParser definitionParser,
        ChallengeCalibrationSource calibrationSource
    ) {
        this.definitionParser = definitionParser;
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
    public ChallengeSelection weekly(LocalDate weekStart, Challenge challenge, Instant selectionTime) {
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
    public ChallengeSelection daily(
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
    private ChallengeSelection create(
        LocalDate weekStart,
        LocalDate day,
        Challenge challenge,
        Instant selectionTime
    ) {
        ChallengeCadence cadence = day == null ? ChallengeCadence.WEEKLY : ChallengeCadence.DAILY;
        ChallengeCalibration calibration = calibrationSource.forWeek(weekStart);
        ChallengeDefinition played = definitionParser.parse(challenge, calibration.difficulty());

        ChallengeSelection selection = new ChallengeSelection();
        selection.setWeekStart(weekStart);
        selection.setCadence(cadence);
        selection.setDay(day);
        selection.setChallenge(challenge);
        selection.setSelectedAt(selectionTime);
        selection.setResolvedConditionsJson(definitionParser.toJson(played.conditions()));

        return selection;
    }
}
