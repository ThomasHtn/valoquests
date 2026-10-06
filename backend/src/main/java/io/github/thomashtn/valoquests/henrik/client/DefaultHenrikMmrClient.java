package io.github.thomashtn.valoquests.henrik.client;

import io.github.thomashtn.valoquests.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import org.springframework.stereotype.Component;

/**
 * Retrieves a player's current competitive rating through Henrik's MMR v3 endpoint.
 */
@Component
public class DefaultHenrikMmrClient implements HenrikMmrClient {

    /**
     * Relative Henrik endpoint used to retrieve the current MMR.
     */
    private static final String CURRENT_MMR_ENDPOINT =
        "/valorant/v3/by-puuid/mmr/{region}/{platform}/{puuid}";

    /**
     * Application-wide Henrik configuration.
     */
    private final HenrikApiProperties properties;

    /**
     * Shared executor sending the request behind the rate limiter and retry policy.
     */
    private final HenrikRequestExecutor requestExecutor;

    /**
     * Creates the Henrik MMR client.
     *
     * @param properties      Henrik API configuration
     * @param requestExecutor shared Henrik request executor
     */
    public DefaultHenrikMmrClient(
        HenrikApiProperties properties,
        HenrikRequestExecutor requestExecutor
    ) {
        this.properties = properties;
        this.requestExecutor = requestExecutor;
    }

    /**
     * Retrieves the current competitive rating for the supplied player PUUID.
     *
     * @param puuid Riot player PUUID
     * @return current MMR response
     */
    @Override
    public HenrikMmrResponse getCurrentMmr(String puuid) {
        HenrikRequestExecutor.requireText(puuid, "puuid");

        return requestExecutor.get(
            "retrieve current MMR for Riot PUUID " + puuid,
            uri -> uri
                .path(CURRENT_MMR_ENDPOINT)
                .build(properties.region(), properties.platform(), puuid),
            HenrikMmrResponse.class
        );
    }
}
