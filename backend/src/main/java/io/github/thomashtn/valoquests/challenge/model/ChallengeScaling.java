package io.github.thomashtn.valoquests.challenge.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries the two anchorings a campaign freezes at opening to scale the catalogue's base targets.
 *
 * @param volumeFactor campaign reference over the reference the catalogue was written for, bounded
 * @param anchors      squad talent anchors, absent entries fall back to the catalogue's base target
 */
public record ChallengeScaling(
    BigDecimal volumeFactor,
    Map<SkillAnchor, BigDecimal> anchors
) {

    /**
     * Scaling that leaves every base target untouched: what applies outside any campaign.
     */
    public static final ChallengeScaling NONE = new ChallengeScaling(BigDecimal.ONE, Map.of());

    /**
     * Creates an immutable scaling.
     */
    public ChallengeScaling {
        Objects.requireNonNull(volumeFactor, "Volume factor must not be null.");
        Objects.requireNonNull(anchors, "Skill anchors must not be null.");

        if (volumeFactor.signum() <= 0) {
            throw new IllegalArgumentException("Volume factor must be positive.");
        }

        anchors = Map.copyOf(anchors);
    }

    /**
     * Returns the squad's value of one anchor.
     *
     * @param anchor anchor to read
     * @return the measured value, empty when the squad has none
     */
    public Optional<BigDecimal> anchor(SkillAnchor anchor) {
        return Optional.ofNullable(anchors.get(anchor));
    }
}
