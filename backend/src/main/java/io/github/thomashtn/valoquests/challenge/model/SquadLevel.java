package io.github.thomashtn.valoquests.challenge.model;

/**
 * Identifies which of a challenge's two written targets a campaign plays against.
 *
 * <p>Replaces the volume factor and the talent anchors. A target used to be computed from nine
 * months of history, which made every number in the catalogue a base nobody could read: the same
 * challenge showed twelve kills to one squad and thirty to another, and a squad at the lower bound
 * saw its daily objectives divided by two and a half. The catalogue now writes both numbers by
 * hand, and a campaign only picks a side.
 */
public enum SquadLevel {

    /**
     * The value written for a squad playing regularly, and the default of a new campaign.
     */
    REFERENCE,

    /**
     * The value written for a squad that clears the reference without effort.
     */
    EXPERT
}
