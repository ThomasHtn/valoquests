package io.github.thomashtn.valorant.tracker.henrik.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.henrik.dto.account.HenrikAccountResponse;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikAccountMapper;
import io.github.thomashtn.valorant.tracker.henrik.model.HenrikAccount;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient-based implementation of the Henrik account client.
 */
@Component
public class DefaultHenrikAccountClient implements HenrikAccountClient {

    /**
     * Henrik account endpoint path.
     */
    private static final String ACCOUNT_ENDPOINT =
        "/valorant/v2/account/{gameName}/{tagLine}";

    /**
     * HTTP client configured specifically for Henrik API calls.
     */
    private final WebClient henrikWebClient;

    /**
     * Shared handler converting Henrik HTTP failures into typed exceptions.
     */
    private final HenrikResponseHandler responseHandler;

    /**
     * Shared executor applying transport conversion and retry rules.
     */
    private final HenrikRequestExecutor requestExecutor;

    /**
     * Mapper isolating the application from the external JSON structure.
     */
    private final HenrikAccountMapper accountMapper;

    /**
     * Creates the Henrik account client.
     *
     * @param henrikWebClient configured Henrik HTTP client
     * @param responseHandler external response error handler
     * @param requestExecutor shared Henrik request executor
     * @param accountMapper external account response mapper
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultHenrikAccountClient(
        WebClient henrikWebClient,
        HenrikResponseHandler responseHandler,
        HenrikRequestExecutor requestExecutor,
        HenrikAccountMapper accountMapper
    ) {
        this.henrikWebClient = henrikWebClient;
        this.responseHandler = responseHandler;
        this.requestExecutor = requestExecutor;
        this.accountMapper = accountMapper;
    }

    /**
     * Resolves a Riot account using its game name and tag line.
     *
     * @param gameName Riot game name
     * @param tagLine Riot tag line
     * @return resolved Riot account
     */
    @Override
    public HenrikAccount getAccount(
        String gameName,
        String tagLine
    ) {
        validateRiotIdPart(gameName, "gameName");
        validateRiotIdPart(tagLine, "tagLine");

        String operationName =
            "resolve Riot account " + gameName + "#" + tagLine;

        HenrikAccountResponse response = requestExecutor.execute(
            operationName,
            () -> executeAccountRequest(gameName, tagLine)
        );

        return accountMapper.toModel(response);
    }

    /**
     * Builds and executes the external Henrik account request.
     *
     * <p>URI variables are encoded by Spring, preventing Riot IDs containing
     * spaces or special characters from corrupting the request path.</p>
     *
     * @param gameName Riot game name
     * @param tagLine Riot tag line
     * @return lazy response publisher
     */
    private Mono<HenrikAccountResponse> executeAccountRequest(
        String gameName,
        String tagLine
    ) {
        return henrikWebClient.get()
            .uri(
                ACCOUNT_ENDPOINT,
                gameName,
                tagLine
            )
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                responseHandler::toException
            )
            .bodyToMono(HenrikAccountResponse.class);
    }

    /**
     * Validates one part of the Riot ID before sending an external request.
     *
     * @param value Riot ID part to validate
     * @param fieldName field name used in the validation message
     * @throws IllegalArgumentException when the value is null or blank
     */
    private void validateRiotIdPart(
        String value,
        String fieldName
    ) {
        if (Objects.requireNonNullElse(value, "").isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }
    }
}
