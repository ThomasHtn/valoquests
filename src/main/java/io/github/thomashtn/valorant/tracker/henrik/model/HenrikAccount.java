package io.github.thomashtn.valorant.tracker.henrik.model;

/**
 * Represents a resolved Riot account independently from the Henrik response
 * structure.
 *
 * @param puuid stable Riot account identifier
 * @param gameName current Riot game name
 * @param tagLine current Riot tag line
 */
public record HenrikAccount(
    String puuid,
    String gameName,
    String tagLine
) {
}
