package io.github.thomashtn.valorant.tracker.henrik.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient-based implementation of the Henrik match-history client.
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
     * Maximum number of matches accepted for one request.
     *
     * <p>This application uses small pages to limit external request duration
     * and simplify incremental synchronization.</p>
     */
    private static final int MAX_PAGE_SIZE = 10;

    /**
     * HTTP client configured for Henrik API calls.
     */
    private final WebClient henrikWebClient;

    /**
     * Application-wide Henrik configuration.
     */
    private final HenrikApiProperties properties;

    /**
     * Converts Henrik HTTP failures into typed application exceptions.
     */
    private final HenrikResponseHandler responseHandler;

    /**
     * Applies common Henrik transport handling and retry rules.
     */
    private final HenrikRequestExecutor requestExecutor;

    /**
     * Creates the Henrik match client.
     *
     * @param henrikWebClient configured Henrik HTTP client
     * @param properties Henrik API configuration
     * @param responseHandler external response error handler
     * @param requestExecutor shared Henrik request executor
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultHenrikMatchClient(
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
        validatePuuid(puuid);
        validatePagination(start, size);

        String operationName =
            "retrieve matches for Riot PUUID " + puuid;

        return requestExecutor.execute(
            operationName,
            () -> executeMatchRequest(puuid, start, size)
        );
    }

    /**
     * Builds and executes the Henrik match-history request.
     *
     * @param puuid Riot's unique player identifier
     * @param start zero-based pagination start index
     * @param size maximum number of matches to retrieve
     * @return lazy Henrik response publisher
     */
    private Mono<HenrikMatchHistoryResponse> executeMatchRequest(
        String puuid,
        int start,
        int size
    ) {
        return henrikWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(MATCH_HISTORY_ENDPOINT)
                .queryParam("start", start)
                .queryParam("size", size)
                .build(
                    properties.region(),
                    properties.platform(),
                    puuid
                )
            )
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                responseHandler::toException
            )
            .bodyToMono(HenrikMatchHistoryResponse.class);
    }

    /**
     * Validates the Riot PUUID before an external request.
     *
     * @param puuid Riot's unique player identifier
     */
    private void validatePuuid(String puuid) {
        if (Objects.requireNonNullElse(puuid, "").isBlank()) {
            throw new IllegalArgumentException(
                "puuid must not be blank"
            );
        }
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
