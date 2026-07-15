package io.github.thomashtn.valorant.tracker.henrik.client;

import java.util.List;

/**
 * Defines the minimum Henrik API operations required by synchronization services.
 */
public interface HenrikApiClient {

    /**
     * Retrieves Valorant account information.
     *
     * @param gameName Riot game name
     * @param tagLine Riot tag line
     * @return matching account data
     */
    AccountData getAccount(String gameName, String tagLine);

    /**
     * Retrieves the current competitive rank for a player.
     *
     * @param puuid Riot player UUID
     * @return current competitive rank data
     */
    RankData getCurrentRank(String puuid);

    /**
     * Retrieves the most recent matches for a player.
     *
     * @param puuid Riot player UUID
     * @return recent match references
     */
    List<MatchData> getRecentMatches(String puuid);

    /**
     * Minimal Valorant account representation returned by the external API.
     *
     * @param puuid Riot player UUID
     * @param gameName Riot game name
     * @param tagLine Riot tag line
     */
    record AccountData(String puuid, String gameName, String tagLine) {
    }

    /**
     * Minimal competitive rank representation returned by the external API.
     *
     * @param tier external competitive tier name
     * @param rankRating current rank rating
     */
    record RankData(String tier, Integer rankRating) {
    }

    /**
     * Minimal match representation returned by the external API.
     *
     * @param matchId external match identifier
     */
    record MatchData(String matchId) {
    }
}
