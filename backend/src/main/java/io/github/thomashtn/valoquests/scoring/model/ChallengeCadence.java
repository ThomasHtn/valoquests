package io.github.thomashtn.valoquests.scoring.model;

/**
 * How often a challenge is drawn, and therefore over which window its progress is measured.
 *
 * <p>Weekly challenges are drawn on Monday for the week, daily ones each morning for that day; the two
 * never share a pool.
 */
public enum ChallengeCadence {
    WEEKLY, DAILY
}
