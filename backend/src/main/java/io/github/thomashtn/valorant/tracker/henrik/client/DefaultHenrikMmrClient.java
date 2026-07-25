package io.github.thomashtn.valorant.tracker.henrik.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.henrik.dto.mmr.HenrikMmrResponse;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient implementation of Henrik's Valorant MMR v3 endpoint.
 */
@Component
public class DefaultHenrikMmrClient implements HenrikMmrClient {

    /**
     * Relative Henrik endpoint used to retrieve the current MMR.
     */
    private static final String CURRENT_MMR_ENDPOINT =
        "/valorant/v3/by-puuid/mmr/{region}/{platform}/{puuid}";

    /**
     * Configured WebClient used for Henrik HTTP requests.
     */
    private final WebClient henrikWebClient;

    /**
     * Typed configuration required by this component.
     */
    private final HenrikApiProperties properties;

    /**
     * Handler used to convert Henrik error responses into typed exceptions.
     */
    private final HenrikResponseHandler responseHandler;

    /**
     * Executor that applies rate limiting, retries and error handling.
     */
    private final HenrikRequestExecutor requestExecutor;

    /**
     * Creates the Henrik MMR client.
     *
     * @param henrikWebClient configured Henrik HTTP client
     * @param properties Henrik API configuration properties
     * @param responseHandler external response error handler
     * @param requestExecutor shared Henrik request executor
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultHenrikMmrClient(
        WebClient henrikWebClient,
        HenrikApiProperties properties,
        HenrikResponseHandler responseHandler,
        HenrikRequestExecutor requestExecutor
    ) {
        this.henrikWebClient = henrikWebClient;
        this.properties = properties;
        this.responseHandler = responseHandler;
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
        validatePuuid(puuid);

        return requestExecutor.execute(
            "retrieve current MMR for Riot PUUID " + puuid,
            () -> executeRequest(puuid)
        );
    }

    private Mono<HenrikMmrResponse> executeRequest(String puuid) {
        return henrikWebClient.get()
            .uri(
                CURRENT_MMR_ENDPOINT,
                properties.region(),
                properties.platform(),
                puuid
            )
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                responseHandler::toException
            )
            .bodyToMono(HenrikMmrResponse.class);
    }

    private void validatePuuid(String puuid) {
        if (Objects.requireNonNullElse(puuid, "").isBlank()) {
            throw new IllegalArgumentException(
                "puuid must not be blank"
            );
        }
    }
}
