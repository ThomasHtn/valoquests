package io.github.thomashtn.valoquests.challenge.model;

/**
 * How often a challenge is drawn, and therefore over which window its progress is measured.
 *
 * <p>A weekly challenge is drawn on Monday and measured over the whole week; a daily one is drawn
 * every morning and measured over that single calendar day. The two never share a pool.
 */
public enum ChallengeCadence {
    WEEKLY, DAILY
}
