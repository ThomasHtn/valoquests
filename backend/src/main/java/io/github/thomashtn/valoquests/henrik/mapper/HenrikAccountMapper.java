package io.github.thomashtn.valoquests.henrik.mapper;

import io.github.thomashtn.valoquests.henrik.dto.account.HenrikAccountResponse;
import io.github.thomashtn.valoquests.henrik.model.HenrikAccount;
import org.springframework.stereotype.Component;

/**
 * Converts Henrik account responses into the stable internal account model.
 */
@Component
public class HenrikAccountMapper {

    /**
     * Converts an external Henrik account response into an internal account.
     *
     * @param response external Henrik account response
     * @return resolved internal Riot account
     * @throws IllegalArgumentException when the response does not contain
     *                                  usable account data
     */
    public HenrikAccount toModel(HenrikAccountResponse response) {
        if (response == null || response.data() == null) {
            throw new IllegalArgumentException(
                "Henrik account response does not contain account data"
            );
        }

        HenrikAccountResponse.HenrikAccountData data = response.data();

        validateRequiredValue(data.puuid(), "PUUID");
        validateRequiredValue(data.gameName(), "game name");
        validateRequiredValue(data.tagLine(), "tag line");

        return new HenrikAccount(
            data.puuid(),
            data.gameName(),
            data.tagLine()
        );
    }

    /**
     * Validates a required value returned by the external API.
     *
     * @param value external value to validate
     * @param fieldName human-readable field name used in the error message
     * @throws IllegalArgumentException when the value is null or blank
     */
    private void validateRequiredValue(
        String value,
        String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "Henrik account response contains an invalid " + fieldName
            );
        }
    }
}
