package io.github.thomashtn.valoquests.scoring.model;

/**
 * Defines the weight class a catalogue boss belongs to.
 *
 * <p>Governs only the base HP resolved by
 * {@link io.github.thomashtn.valoquests.scoring.ScoringRuleset#bossBaseHp}, not the weekly
 * difficulty modifier, which is collective and evolves independently of which boss was drawn.
 */
public enum BossCategory {
    MINOR,
    STANDARD,
    ELITE
}
