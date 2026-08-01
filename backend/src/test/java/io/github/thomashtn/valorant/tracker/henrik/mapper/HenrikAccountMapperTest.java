package io.github.thomashtn.valorant.tracker.henrik.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.thomashtn.valorant.tracker.henrik.dto.account.HenrikAccountResponse;
import io.github.thomashtn.valorant.tracker.henrik.model.HenrikAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HenrikAccountMapper}.
 */
class HenrikAccountMapperTest {

    /**
     * Mapper under test.
     */
    private HenrikAccountMapper mapper;

    /**
     * Creates a fresh mapper before each test.
     */
    @BeforeEach
    void setUp() {
        mapper = new HenrikAccountMapper();
    }

    /**
     * Verifies that a valid Henrik response is converted into the internal
     * account model.
     */
    @Test
    void shouldMapValidAccountResponse() {
        HenrikAccountResponse response = createResponse(
            "test-puuid",
            "Psilonnix",
            "EUW"
        );

        HenrikAccount result = mapper.toModel(response);

        assertThat(result)
            .isEqualTo(
                new HenrikAccount(
                    "test-puuid",
                    "Psilonnix",
                    "EUW"
                )
            );
    }

    /**
     * Verifies that a null response is rejected.
     */
    @Test
    void shouldRejectNullResponse() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(null))
            .withMessage(
                "Henrik account response does not contain account data"
            );
    }

    /**
     * Verifies that a response without account data is rejected.
     */
    @Test
    void shouldRejectResponseWithoutData() {
        HenrikAccountResponse response = new HenrikAccountResponse(
            200,
            null
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response does not contain account data"
            );
    }

    /**
     * Verifies that a missing PUUID is rejected.
     */
    @Test
    void shouldRejectNullPuuid() {
        HenrikAccountResponse response = createResponse(
            null,
            "Psilonnix",
            "EUW"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response contains an invalid PUUID"
            );
    }

    /**
     * Verifies that a blank PUUID is rejected.
     */
    @Test
    void shouldRejectBlankPuuid() {
        HenrikAccountResponse response = createResponse(
            " ",
            "Psilonnix",
            "EUW"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response contains an invalid PUUID"
            );
    }

    /**
     * Verifies that a missing game name is rejected.
     */
    @Test
    void shouldRejectNullGameName() {
        HenrikAccountResponse response = createResponse(
            "test-puuid",
            null,
            "EUW"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response contains an invalid game name"
            );
    }

    /**
     * Verifies that a blank game name is rejected.
     */
    @Test
    void shouldRejectBlankGameName() {
        HenrikAccountResponse response = createResponse(
            "test-puuid",
            " ",
            "EUW"
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response contains an invalid game name"
            );
    }

    /**
     * Verifies that a missing tag line is rejected.
     */
    @Test
    void shouldRejectNullTagLine() {
        HenrikAccountResponse response = createResponse(
            "test-puuid",
            "Psilonnix",
            null
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response contains an invalid tag line"
            );
    }

    /**
     * Verifies that a blank tag line is rejected.
     */
    @Test
    void shouldRejectBlankTagLine() {
        HenrikAccountResponse response = createResponse(
            "test-puuid",
            "Psilonnix",
            " "
        );

        assertThatIllegalArgumentException()
            .isThrownBy(() -> mapper.toModel(response))
            .withMessage(
                "Henrik account response contains an invalid tag line"
            );
    }

    /**
     * Creates a Henrik account response for a test scenario.
     *
     * @param puuid Riot account identifier
     * @param gameName Riot game name
     * @param tagLine Riot tag line
     * @return external Henrik response
     */
    private HenrikAccountResponse createResponse(
        String puuid,
        String gameName,
        String tagLine
    ) {
        return new HenrikAccountResponse(
            200,
            new HenrikAccountResponse.HenrikAccountData(
                puuid,
                gameName,
                tagLine
            )
        );
    }
}
