package io.github.thomashtn.valoquests.henrik.client;

import io.github.thomashtn.valoquests.henrik.dto.account.HenrikAccountResponse;
import io.github.thomashtn.valoquests.henrik.mapper.HenrikAccountMapper;
import io.github.thomashtn.valoquests.henrik.model.HenrikAccount;
import org.springframework.stereotype.Component;

/**
 * Resolves a Riot account from its Riot ID through Henrik's account endpoint.
 */
@Component
public class DefaultHenrikAccountClient implements HenrikAccountClient {

    /**
     * Henrik account endpoint path.
     */
    private static final String ACCOUNT_ENDPOINT =
        "/valorant/v2/account/{gameName}/{tagLine}";

    /**
     * Shared executor sending the request behind the rate limiter and retry policy.
     */
    private final HenrikRequestExecutor requestExecutor;

    /**
     * Mapper isolating the application from the external JSON structure.
     */
    private final HenrikAccountMapper accountMapper;

    /**
     * Creates the Henrik account client.
     *
     * @param requestExecutor shared Henrik request executor
     * @param accountMapper   external account response mapper
     */
    public DefaultHenrikAccountClient(
        HenrikRequestExecutor requestExecutor,
        HenrikAccountMapper accountMapper
    ) {
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
        HenrikRequestExecutor.requireText(gameName, "gameName");
        HenrikRequestExecutor.requireText(tagLine, "tagLine");

        HenrikAccountResponse response = requestExecutor.get(
            "resolve Riot account " + gameName + "#" + tagLine,
            uri -> uri.path(ACCOUNT_ENDPOINT).build(gameName, tagLine),
            HenrikAccountResponse.class
        );

        return accountMapper.toModel(response);
    }
}
