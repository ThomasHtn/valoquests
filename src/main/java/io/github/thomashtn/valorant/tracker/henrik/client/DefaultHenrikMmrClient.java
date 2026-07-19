package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.henrik.dto.mmr.HenrikMmrResponse;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** WebClient implementation of Henrik's Valorant MMR v3 endpoint. */
@Component
public class DefaultHenrikMmrClient implements HenrikMmrClient {

    private static final String CURRENT_MMR_ENDPOINT =
        "/valorant/v3/by-puuid/mmr/{region}/{platform}/{puuid}";

    private final WebClient henrikWebClient;
    private final HenrikApiProperties properties;
    private final HenrikResponseHandler responseHandler;
    private final HenrikRequestExecutor requestExecutor;

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
