package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeCalibrationSource;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.service.ScoringRuleset;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices what each player's validated challenges are worth in the weekly ranking.
 *
 * <p>Every player is priced whatever their status; the ranking decides who keeps the points. The week's
 * reference in force is used, so a challenge validated between campaigns still pays.
 */
@Service
@Transactional(readOnly = true)
public class ChallengePointsReader {

    /**
     * Repository holding the week's challenge progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Scoring table pricing one validated challenge.
     */
    private final ScoringRuleset ruleset;

    /**
     * Source of the reference in force for a week.
     */
    private final ChallengeCalibrationSource calibrationSource;

    /**
     * Creates the challenge points reader.
     *
     * @param progressRepository player challenge progress repository
     * @param ruleset            scoring ruleset
     * @param calibrationSource  challenge calibration source
     */
    public ChallengePointsReader(
        PlayerChallengeProgressRepository progressRepository,
        ScoringRuleset ruleset,
        ChallengeCalibrationSource calibrationSource
    ) {
        this.progressRepository = progressRepository;
        this.ruleset = ruleset;
        this.calibrationSource = calibrationSource;
    }

    /**
     * Tallies one week's validated challenges per player.
     *
     * @param weekStart Monday identifying the week
     * @return each player's tally, players who validated nothing omitted
     */
    public Map<Long, ChallengeTally> read(LocalDate weekStart) {
        ChallengeCalibration calibration = calibrationSource.forWeek(weekStart);
        Map<Long, ChallengeTally> tallies = new HashMap<>();

        for (PlayerChallengeProgress progress : progressRepository
            .findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(weekStart)) {

            if (!progress.isCompleted()) {
                continue;
            }

            ChallengeSelection selection = progress.getSelection();
            boolean daily = selection.getCadence() == ChallengeCadence.DAILY;
            int points = ruleset.challengeReward(
                selection.getCadence(),
                selection.getChallenge().getTier(),
                calibration
            );

            tallies.merge(
                progress.getPlayer().getId(),
                new ChallengeTally(points, daily ? 0 : 1, daily ? 1 : 0),
                ChallengeTally::plus
            );
        }

        return tallies;
    }

    /**
     * What one player's validated challenges add up to over a week.
     *
     * @param points         ranking points, one per wounded, priced at the reference in force
     * @param completedWeekly weekly challenges validated
     * @param completedDaily  daily challenges validated
     */
    public record ChallengeTally(int points, int completedWeekly, int completedDaily) {

        /**
         * The tally of a player who validated nothing.
         */
        public static final ChallengeTally NONE = new ChallengeTally(0, 0, 0);

        /**
         * Adds another tally to this one.
         *
         * @param other tally to add
         * @return the sum
         */
        public ChallengeTally plus(ChallengeTally other) {
            return new ChallengeTally(
                points + other.points,
                completedWeekly + other.completedWeekly,
                completedDaily + other.completedDaily
            );
        }
    }
}
