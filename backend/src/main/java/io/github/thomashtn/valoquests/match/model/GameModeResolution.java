package io.github.thomashtn.valoquests.match.model;

/**
 * A resolved game mode paired with how confidently it was determined.
 *
 * @param gameMode resolved mode
 * @param source   identifier tier that resolved it
 */
public record GameModeResolution(GameMode gameMode, GameModeSource source) {
}
