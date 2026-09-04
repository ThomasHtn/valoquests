package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeCalibrationSource;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices what each player's validated challenges are worth in the weekly ranking.
 *
 * <p>Every player is priced, whatever their status: deciding who keeps their points belongs to the
 * ranking, which is the one place that knows who takes part. The reference is the one in force for
 * the week, so a challenge validated between two campaigns pays at the last campaign's reference
 * rather than at nothing.
 */
@Service
@Transactional(readOnly = true)
public class ChallengePointsReader {

    /**
     * Repository holding the week's challenge progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Barème pricing one validated challenge.
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
     * Returns the reference one week's challenges are priced at.
     *
     * @param weekStart Monday identifying the week
     * @return the reference in force
     */
    public int referenceFor(LocalDate weekStart) {
        return calibrationSource.forWeek(weekStart).reference();
    }

    /**
     * Prices one selection.
     *
     * @param selection selected challenge
     * @param reference reference in force for its week
     * @return the ranking points validating it earns
     */
    public int pointsOf(WeeklyChallenge selection, int reference) {
        double weight = ruleset.challengeWeight(
            selection.getChallenge().getCadence(),
            selection.getChallenge().getDifficulty()
        );

        return ruleset.challengeRankingPoints(reference, weight);
    }

    /**
     * Tallies one week's validated challenges per player.
     *
     * @param weekStart Monday identifying the week
     * @return each player's tally, players who validated nothing omitted
     */
    public Map<Long, ChallengeTally> read(LocalDate weekStart) {
        int reference = referenceFor(weekStart);
        Map<Long, ChallengeTally> tallies = new HashMap<>();

        for (PlayerChallengeProgress progress : progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(weekStart)) {

            if (!progress.isCompleted()) {
                continue;
            }

            WeeklyChallenge selection = progress.getWeeklyChallenge();
            boolean daily = selection.getCadence() == ChallengeCadence.DAILY;

            tallies.merge(
                progress.getPlayer().getId(),
                new ChallengeTally(pointsOf(selection, reference), daily ? 0 : 1, daily ? 1 : 0),
                ChallengeTally::plus
            );
        }

        return tallies;
    }

    /**
     * What one player's validated challenges add up to over a week.
     *
     * @param points         ranking points, priced at the reference in force
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
