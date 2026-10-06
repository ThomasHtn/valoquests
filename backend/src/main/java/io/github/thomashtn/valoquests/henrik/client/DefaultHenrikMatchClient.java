package io.github.thomashtn.valoquests.henrik.client;

import io.github.thomashtn.valoquests.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import org.springframework.stereotype.Component;

/**
 * Retrieves pages of a player's match history through Henrik's v4 endpoint.
 */
@Component
public class DefaultHenrikMatchClient implements HenrikMatchClient {

    /**
     * Henrik v4 match-history endpoint using a Riot PUUID.
     */
    private static final String MATCH_HISTORY_ENDPOINT =
        "/valorant/v4/by-puuid/matches/{region}/{platform}/{puuid}";

    /**
     * Minimum number of matches accepted for one request.
     */
    private static final int MIN_PAGE_SIZE = 1;

    /**
     * Application-wide Henrik configuration.
     */
    private final HenrikApiProperties properties;

    /**
     * Shared executor sending the request behind the rate limiter and retry policy.
     */
    private final HenrikRequestExecutor requestExecutor;

    /**
     * Creates the Henrik match client.
     *
     * @param properties      Henrik API configuration
     * @param requestExecutor shared Henrik request executor
     */
    public DefaultHenrikMatchClient(
        HenrikApiProperties properties,
        HenrikRequestExecutor requestExecutor
    ) {
        this.properties = properties;
        this.requestExecutor = requestExecutor;
    }

    /**
     * Retrieves one page of recent matches for a player.
     *
     * @param puuid Riot's unique player identifier
     * @param start zero-based pagination start index
     * @param size maximum number of matches to retrieve
     * @return decoded Henrik match-history response
     */
    @Override
    public HenrikMatchHistoryResponse getMatches(
        String puuid,
        int start,
        int size
    ) {
        HenrikRequestExecutor.requireText(puuid, "puuid");
        validatePagination(start, size);

        return requestExecutor.get(
            "retrieve matches for Riot PUUID " + puuid,
            uri -> uri
                .path(MATCH_HISTORY_ENDPOINT)
                .queryParam("start", start)
                .queryParam("size", size)
                .build(properties.region(), properties.platform(), puuid),
            HenrikMatchHistoryResponse.class
        );
    }

    /**
     * Validates match-history pagination parameters.
     *
     * @param start pagination start index
     * @param size requested page size
     */
    private void validatePagination(int start, int size) {
        if (start < 0) {
            throw new IllegalArgumentException(
                "start must be greater than or equal to zero"
            );
        }

        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "size must be between "
                    + MIN_PAGE_SIZE
                    + " and "
                    + MAX_PAGE_SIZE
            );
        }
    }
}
