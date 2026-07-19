package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.model.HenrikAccount;

/**
 * Defines Henrik operations related to Riot account resolution.
 */
public interface HenrikAccountClient {

    /**
     * Resolves a Riot account from its game name and tag line.
     *
     * @param gameName Riot game name
     * @param tagLine Riot tag line
     * @return resolved Riot account
     */
    HenrikAccount getAccount(String gameName, String tagLine);
}
