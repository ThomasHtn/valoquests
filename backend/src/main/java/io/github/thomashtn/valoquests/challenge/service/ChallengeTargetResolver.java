package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Resolves a catalogue definition's base targets against one campaign's scaling.
 *
 * <p>The only place the scaling table lives. The field being scaled decides the anchoring, not the
 * metric: a cumulated target follows the campaign's volume, a per-match bar or a rate follows the
 * squad's talent anchor times a difficulty coefficient, and anything counting days, agents or
 * modes is never scaled, since a week is seven days long for everyone.
 *
 * <p>Resolved numbers are rounded to a readable step. The document's steps (five under one hundred,
 * ten under one thousand, one thousand beyond) are kept, with two finer steps it did not spell out:
 * integers under twenty, so that "play 3 matches" scaled by 0.47 gives 1 rather than 0, and
 * hundreds between one and ten thousand, so that a two percent change of reference does not move a
 * 4 500 damage target by a fifth.
 */
@Component
public class ChallengeTargetResolver {

    /**
     * Scale kept on a resolved ratio, enough for a kill-to-death ratio to read as "1,10".
     */
    private static final int RATE_SCALE = 2;

    /**
     * Rounding step ladder for volume targets: below each bound, round to the step beside it.
     */
    private static final long[][] VOLUME_STEPS = {
        {20, 1}, {100, 5}, {1_000, 10}, {10_000, 100}, {Long.MAX_VALUE, 1_000},
    };

    /**
     * Rounding step ladder for talent bars that are counts rather than rates.
     */
    private static final long[][] TALENT_STEPS = {
        {100, 1}, {1_000, 5}, {Long.MAX_VALUE, 100},
    };

    /**
     * Resolves every number of one definition.
     *
     * @param definition base definition read from the catalogue
     * @param cadence    cadence of the challenge being drawn
     * @param difficulty difficulty of the challenge, {@code null} for a daily one
     * @param scaling    scaling in force, {@link ChallengeScaling#NONE} outside any campaign
     * @return resolved definition, the base one when the scaling is neutral
     */
    public ChallengeDefinition resolve(
        ChallengeDefinition definition,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        ChallengeScaling scaling
    ) {
        Objects.requireNonNull(definition, "Challenge definition must not be null.");
        Objects.requireNonNull(cadence, "Challenge cadence must not be null.");
        Objects.requireNonNull(scaling, "Challenge scaling must not be null.");

        if (ChallengeScaling.NONE.equals(scaling)) {
            return definition;
        }

        return new ChallengeDefinition(
            definition.schemaVersion(),
            definition.progressMode(),
            definition.conditions().stream()
                .map(condition -> resolve(
                    condition,
                    definition.progressMode(),
                    cadence,
                    difficulty,
                    scaling
                ))
                .toList()
        );
    }

    /**
     * Resolves one condition.
     *
     * @param condition    base condition
     * @param progressMode progress mode of the owning definition
     * @param cadence      cadence of the challenge
     * @param difficulty   difficulty of the challenge, {@code null} for a daily one
     * @param scaling      scaling in force
     * @return resolved condition
     */
    private ChallengeCondition resolve(
        ChallengeCondition condition,
        ProgressMode progressMode,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        ChallengeScaling scaling
    ) {
        BigDecimal target = resolveTarget(condition, progressMode, cadence, difficulty, scaling);
        Integer occurrences = condition.occurrences();

        // A daily challenge is decided in one or two matches for everyone, so its occurrences never
        // follow the volume the way a weekly one's do.
        if (occurrences != null && cadence == ChallengeCadence.WEEKLY) {
            occurrences = scaleCount(occurrences, scaling);
        }

        Integer minimumMatches = condition.minimumMatches();

        if (minimumMatches != null) {
            minimumMatches = scaleCount(minimumMatches, scaling);
        }

        return condition.withNumbers(target, occurrences, minimumMatches);
    }

    /**
     * Resolves one condition's target.
     *
     * @param condition    base condition
     * @param progressMode progress mode of the owning definition
     * @param cadence      cadence of the challenge
     * @param difficulty   difficulty of the challenge, {@code null} for a daily one
     * @param scaling      scaling in force
     * @return resolved target
     */
    private BigDecimal resolveTarget(
        ChallengeCondition condition,
        ProgressMode progressMode,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        ChallengeScaling scaling
    ) {
        if (isFixed(condition, progressMode, cadence)) {
            return condition.target();
        }

        if (isTalentBar(condition)) {
            return SkillAnchor.of(condition.metric(), condition.effectiveGameMode())
                .flatMap(scaling::anchor)
                .map(anchor -> talentTarget(condition, anchor, cadence, difficulty))
                .orElse(condition.target());
        }

        return roundToStep(condition.target().multiply(scaling.volumeFactor()), VOLUME_STEPS);
    }

    /**
     * Tells whether a target must never be scaled.
     *
     * <p>Days, agents and modes are the same for every squad. A number of matches inside one day
     * is fixed too, on either cadence: the daily pool is written as one or two matches, and a
     * "three ranked matches in one day" bar measures a day, not a week's volume.
     *
     * @param condition    base condition
     * @param progressMode progress mode of the owning definition
     * @param cadence      cadence of the challenge
     * @return {@code true} when the base target is kept as is
     */
    private boolean isFixed(
        ChallengeCondition condition,
        ProgressMode progressMode,
        ChallengeCadence cadence
    ) {
        if (condition.metric() == ChallengeMetric.PLAY_DAY) {
            return true;
        }

        ChallengeGroupBy groupBy = condition.groupBy();

        if (groupBy == ChallengeGroupBy.AGENT || groupBy == ChallengeGroupBy.GAME_MODE) {
            return true;
        }

        if (groupBy == ChallengeGroupBy.PLAY_DAY && progressMode == ProgressMode.DISTINCT_COUNT) {
            return true;
        }

        boolean withinOneDay = groupBy == ChallengeGroupBy.PLAY_DAY
            || cadence == ChallengeCadence.DAILY;

        return withinOneDay && condition.isMatchCountMetric();
    }

    /**
     * Tells whether a target is a bar one match must clear, or a rate.
     *
     * @param condition base condition
     * @return {@code true} when the target scales on a talent anchor
     */
    private boolean isTalentBar(ChallengeCondition condition) {
        return condition.scope() == ChallengeScope.PER_MATCH || condition.isRateMetric();
    }

    /**
     * Prices a talent bar from the squad's anchor and the tier's coefficient.
     *
     * @param condition  base condition
     * @param anchor     squad's measured anchor
     * @param cadence    cadence of the challenge
     * @param difficulty difficulty of the challenge, {@code null} for a daily one
     * @return resolved bar
     */
    private BigDecimal talentTarget(
        ChallengeCondition condition,
        BigDecimal anchor,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty
    ) {
        BigDecimal scaled = anchor.multiply(talentCoefficient(cadence, difficulty));

        if (condition.isRatioMetric()) {
            return scaled.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }

        return roundToStep(scaled, TALENT_STEPS);
    }

    /**
     * Returns the coefficient applied to a talent anchor for one tier.
     *
     * @param cadence    cadence of the challenge
     * @param difficulty difficulty of the challenge, {@code null} for a daily one
     * @return multiplier of the squad's median
     */
    private BigDecimal talentCoefficient(ChallengeCadence cadence, ChallengeDifficulty difficulty) {
        if (cadence == ChallengeCadence.DAILY || difficulty == null) {
            return new BigDecimal("0.85");
        }

        return switch (difficulty) {
            case EASY -> new BigDecimal("0.90");
            case NORMAL -> BigDecimal.ONE;
            case MEDIUM -> new BigDecimal("1.08");
            case HARD -> new BigDecimal("1.18");
            case VERY_HARD -> new BigDecimal("1.32");
        };
    }

    /**
     * Scales a match count by the volume factor, never below one.
     *
     * @param count   base count
     * @param scaling scaling in force
     * @return resolved count
     */
    private int scaleCount(int count, ChallengeScaling scaling) {
        return roundToStep(BigDecimal.valueOf(count).multiply(scaling.volumeFactor()), VOLUME_STEPS)
            .intValueExact();
    }

    /**
     * Rounds a value to the readable step its magnitude calls for, never below one.
     *
     * @param value value to round
     * @param steps ladder of (upper bound, step) pairs
     * @return rounded value
     */
    private BigDecimal roundToStep(BigDecimal value, long[][] steps) {
        for (long[] step : steps) {
            if (value.compareTo(BigDecimal.valueOf(step[0])) < 0) {
                BigDecimal stepValue = BigDecimal.valueOf(step[1]);
                BigDecimal rounded = value.divide(stepValue, 0, RoundingMode.HALF_UP)
                    .multiply(stepValue);

                return rounded.max(BigDecimal.ONE);
            }
        }

        throw new IllegalStateException("Rounding ladder must end with an unbounded step.");
    }
}
